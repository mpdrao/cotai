package dev.cotai.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record ChargeRequest(UUID reservationId, long amountCents, String description, Instant expiresAt) {
}
