package dev.cotai.reservation;

import dev.cotai.reservation.dto.ReservationCreated;

/** Result of {@code reserve}: {@code replay} is true when the Idempotency-Key was already used. */
public record ReserveOutcome(ReservationCreated body, boolean replay) {
}
