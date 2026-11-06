package dev.cotai.reservation.dto;

import dev.cotai.reservation.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationView(UUID reservationId, String itemName, ReservationStatus status, int quantity,
                              long amountCents, Instant expiresAt, PaymentInfo payment) {
}
