package dev.cotai.reservation;

import dev.cotai.payment.PaymentService;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReserveOutcome;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates "reserve, then open the charge". Deliberately NOT transactional: the reservation
 * commits first (releasing the item row lock), and only then do we call the payment gateway.
 */
@Service
@RequiredArgsConstructor
public class ReservationFlow {

    private final ReservationService reservations;
    private final PaymentService payments;

    public ReserveOutcome reserve(Long itemId, ReserveRequest cmd, UUID idempotencyKey) {
        ReserveOutcome outcome;
        try {
            outcome = reservations.reserve(itemId, cmd, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            // Two simultaneous requests with the same key: the loser hit the UNIQUE index, re-reads and replays (EC06).
            return new ReserveOutcome(reservations.replayByKey(idempotencyKey, cmd, itemId), true);
        }
        if (outcome.replay()) {
            return outcome;
        }
        ReservationCreated created = outcome.body();
        return new ReserveOutcome(created.withPayment(payments.openCharge(created.reservationId())), false);
    }
}
