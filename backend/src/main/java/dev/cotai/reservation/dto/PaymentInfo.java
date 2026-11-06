package dev.cotai.reservation.dto;

import dev.cotai.payment.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentInfo(UUID paymentId, PaymentStatus status, long amountCents, String providerReference,
                          String checkoutUrl, String qrCodePayload, Instant expiresAt) {
}
