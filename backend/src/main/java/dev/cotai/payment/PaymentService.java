package dev.cotai.payment;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.payment.PaymentGateway.Charge;
import dev.cotai.payment.PaymentGateway.ChargeRequest;
import dev.cotai.reservation.ReservationDtos.PaymentInfo;
import dev.cotai.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public record NotificationPayload(String eventId, String type, String providerReference,
                                      Long amountCents, Instant occurredAt) {
    }

    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final ReservationService reservations;
    private final TransactionTemplate tx;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public PaymentInfo openCharge(UUID reservationId) {
        ChargeRequest request = tx.execute(status -> {
            Payment p = payments.findByReservationIdWithDetails(reservationId).orElseThrow();
            return new ChargeRequest(reservationId, p.getAmountCents(),
                    p.getReservation().getItem().getName() + " x" + p.getReservation().getQuantity()
                            + " - " + p.getReservation().getItem().getGiftList().getTitle(),
                    p.getExpiresAt());
        });

        Charge charge;
        try {
            charge = gateway.createCharge(request);
        } catch (RuntimeException e) {
            log.warn("Gateway failed for reservation {}", reservationId, e);
            tx.executeWithoutResult(status -> {
                Payment p = payments.findByReservationIdWithDetails(reservationId).orElseThrow();
                payments.transition(p.getId(), PaymentStatus.CREATED, PaymentStatus.FAILED);
                reservations.cancelDeclined(reservationId);
            });
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE,
                    "The payment provider is unavailable. The reservation was cancelled.");
        }

        return tx.execute(status -> {
            Payment p = payments.findByReservationIdWithDetails(reservationId).orElseThrow();
            payments.markPending(p.getId(), charge.providerReference(), charge.checkoutUrl(), charge.qrCodePayload());
            return ReservationService.toInfo(payments.findByReservationIdWithDetails(reservationId).orElseThrow());
        });
    }

    public NotificationOutcome handleNotification(byte[] rawBody, String signatureHeader) {
        if (!gateway.isSignatureValid(rawBody, signatureHeader)) {
            throw new BusinessException(ErrorCode.INVALID_SIGNATURE, "Missing or invalid X-Signature.");
        }
        NotificationPayload payload = parse(rawBody);
        String rawText = new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);
        return tx.execute(status -> apply(payload, rawText));
    }

    private NotificationOutcome apply(NotificationPayload n, String rawText) {
        if (!processedEventIds.add(n.eventId())) {
            return NotificationOutcome.DUPLICATE;
        }
        Payment payment = payments.findByProviderReference(n.providerReference()).orElse(null);
        return decide(n, payment);
    }

    private NotificationOutcome decide(NotificationPayload n, Payment payment) {
        if (payment == null || n.amountCents() == null || !n.amountCents().equals(payment.getAmountCents())) {
            return NotificationOutcome.REJECTED;
        }
        UUID reservationId = payment.getReservation().getId();
        return switch (n.type()) {
            case "payment.paid" -> onPaid(payment, reservationId);
            case "payment.declined" -> onDeclined(payment, reservationId);
            default -> NotificationOutcome.REJECTED;
        };
    }

    private NotificationOutcome onPaid(Payment payment, UUID reservationId) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return NotificationOutcome.DUPLICATE;
        }
        if (reservations.confirmPaid(reservationId)) {
            payments.markPaid(payment.getId(), clock.instant());
            return NotificationOutcome.APPLIED;
        }
        if (payments.transition(payment.getId(), PaymentStatus.PENDING, PaymentStatus.REFUND_REQUIRED) == 0) {
            payments.transition(payment.getId(), PaymentStatus.EXPIRED, PaymentStatus.REFUND_REQUIRED);
        }
        return NotificationOutcome.LATE_PAYMENT;
    }

    private NotificationOutcome onDeclined(Payment payment, UUID reservationId) {
        payments.transition(payment.getId(), PaymentStatus.PENDING, PaymentStatus.DECLINED);
        reservations.cancelDeclined(reservationId);
        return NotificationOutcome.APPLIED;
    }

    private NotificationPayload parse(byte[] rawBody) {
        try {
            NotificationPayload p = jsonMapper.readValue(rawBody, NotificationPayload.class);
            if (p.eventId() == null || p.type() == null || p.providerReference() == null) {
                throw new IllegalArgumentException("missing required fields");
            }
            return p;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Body is not a valid payment notification.");
        }
    }
}
