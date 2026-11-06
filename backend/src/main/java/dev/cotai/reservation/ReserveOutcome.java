package dev.cotai.reservation;

import dev.cotai.reservation.dto.ReservationCreated;

public record ReserveOutcome(ReservationCreated body, boolean replay) {
}
