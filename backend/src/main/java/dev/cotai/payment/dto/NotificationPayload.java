package dev.cotai.payment.dto;

import java.time.Instant;

public record NotificationPayload(String eventId, String type, String providerReference,
                                  Long amountCents, Instant occurredAt) {
}
