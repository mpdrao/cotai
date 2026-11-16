package dev.cotai.payment;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.payment.PaymentGateway.Charge;
import dev.cotai.payment.PaymentGateway.ChargeRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
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
    private final TransactionTemplate tx;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * UC11. On a gateway error the payment becomes FAILED and PAYMENT_GATEWAY_UNAVAILABLE is thrown;
     * the caller cancels the reservation.
     */
    public PaymentInfo openCharge(UUID reservationId) {
        ChargeRequest request = tx.execute(status -> {
            Payment p = payments.findByReservationId(reservationId).orElseThrow();
            return new ChargeRequest(reservationId, p.getAmountCents(),
                    payments.chargeDescription(reservationId), p.getExpiresAt());
        });

        Charge charge;
        try {
            charge = gateway.createCharge(request);
        } catch (RuntimeException e) {
            log.warn("Gateway failed for reservation {}", reservationId, e);
            tx.executeWithoutResult(status -> {
                Payment p = payments.findByReservationId(reservationId).orElseThrow();
                payments.transition(p.getId(), PaymentStatus.CREATED, PaymentStatus.FAILED);
            });
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE,
                    "The payment provider is unavailable. The reservation was cancelled.");
        }

        return tx.execute(status -> {
            Payment p = payments.findByReservationId(reservationId).orElseThrow();
            payments.markPending(p.getId(), charge.providerReference(), charge.checkoutUrl(), charge.qrCodePayload());
            return PaymentInfo.of(payments.findById(p.getId()).orElseThrow());
        });
    }

    /**
     * UC16. Checks the signature over the RAW bytes before any JSON binding. Empty when the same
     * event was already delivered.
     */
    public Optional<NotificationPayload> verifyNotification(byte[] rawBody, String signatureHeader) {
        if (!gateway.isSignatureValid(rawBody, signatureHeader)) {
            throw new BusinessException(ErrorCode.INVALID_SIGNATURE, "Missing or invalid X-Signature.");
        }
        NotificationPayload payload = parse(rawBody);
        if (!processedEventIds.add(payload.eventId())) {
            return Optional.empty();
        }
        return Optional.of(payload);
    }

    /** PENDING on time, or EXPIRED when the money arrives after the deadline. */
    public void markPaid(UUID paymentId) {
        payments.markPaid(paymentId, clock.instant());
    }

    public void decline(UUID paymentId) {
        payments.transition(paymentId, PaymentStatus.PENDING, PaymentStatus.DECLINED);
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
