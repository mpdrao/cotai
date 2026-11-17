package dev.cotai.reservation;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.payment.Payment;
import dev.cotai.payment.PaymentService;
import dev.cotai.payment.PaymentService.NotificationPayload;
import dev.cotai.payment.PaymentStatus;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReservationView;
import dev.cotai.reservation.ReservationDtos.ReserveOutcome;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Boundary between the reservation transaction and the payment provider (US-03 and US-04). */
@Service
@RequiredArgsConstructor
public class ReservationFlow {

    private static final Logger log = LoggerFactory.getLogger(ReservationFlow.class);

    private final ReservationService reservations;
    private final PaymentService payments;

    public ReserveOutcome reserve(Long itemId, ReserveRequest cmd, UUID idempotencyKey) {
        ReserveOutcome outcome;
        try {
            outcome = reservations.reserve(itemId, cmd, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            return new ReserveOutcome(reservations.replayByKey(idempotencyKey, cmd, itemId), true);
        }
        if (outcome.replay()) {
            return outcome;
        }
        ReservationCreated created = outcome.body();
        try {
            return new ReserveOutcome(created.withPayment(payments.openCharge(created.reservationId())), false);
        } catch (BusinessException e) {
            if (e.code() == ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE) {
                reservations.cancelDeclined(created.reservationId());
            }
            throw e;
        }
    }

    public ReservationView get(UUID reservationId, String manageToken) {
        return reservations.get(reservationId, manageToken);
    }

    public void cancel(UUID reservationId, String manageToken) {
        reservations.cancel(reservationId, manageToken);
    }

    /**
     * UC15. Returns normally (-> HTTP 204) for every signed, parseable notification, including
     * duplicates, late payments and unknown references, so the provider stops retrying.
     */
    @Transactional
    public void handlePaymentNotification(byte[] rawBody, String signatureHeader) {
        NotificationPayload n = payments.verifyNotification(rawBody, signatureHeader).orElse(null);
        if (n == null) {
            return; // duplicate delivery
        }
        Reservation reservation = reservations.findByPaymentReference(n.providerReference()).orElse(null);
        if (reservation == null || n.amountCents() == null
                || n.amountCents() != reservation.getPayment().getAmountCents()) {
            log.warn("Ignoring notification {}: unknown reference or amount mismatch", n.eventId());
            return;
        }
        Payment payment = reservation.getPayment();
        switch (n.type()) {
            case "payment.paid" -> {
                if (payment.getStatus() == PaymentStatus.PAID) {
                    return;
                }
                boolean confirmed = reservations.confirmPaid(reservation.getId());
                payments.markPaid(payment.getId());
                if (!confirmed) {
                    // The reservation was no longer PENDING or past its deadline: it is not reopened.
                    log.warn("Late payment {} for reservation {}", payment.getId(), reservation.getId());
                }
            }
            case "payment.declined" -> {
                payments.decline(payment.getId());
                reservations.cancelDeclined(reservation.getId());
            }
            default -> log.warn("Ignoring notification {} of unknown type {}", n.eventId(), n.type());
        }
    }
}
