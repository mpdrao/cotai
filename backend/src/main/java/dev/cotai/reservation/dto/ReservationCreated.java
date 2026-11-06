package dev.cotai.reservation.dto;

import dev.cotai.reservation.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationCreated(UUID reservationId, String manageToken, ReservationStatus status, int quantity,
                                 long unitPriceCents, long amountCents, Instant expiresAt, PaymentInfo payment) {

    public ReservationCreated withPayment(PaymentInfo newPayment) {
        return new ReservationCreated(reservationId, manageToken, status, quantity, unitPriceCents,
                amountCents, expiresAt, newPayment);
    }
}
