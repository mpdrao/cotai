package dev.cotai.reservation;

import dev.cotai.payment.PaymentService;
import dev.cotai.reservation.dto.ReservationCreated;
import dev.cotai.reservation.dto.ReserveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

            return new ReserveOutcome(reservations.replayByKey(idempotencyKey, cmd, itemId), true);
        }
        if (outcome.replay()) {
            return outcome;
        }
        ReservationCreated created = outcome.body();
        return new ReserveOutcome(created.withPayment(payments.openCharge(created.reservationId())), false);
    }
}
