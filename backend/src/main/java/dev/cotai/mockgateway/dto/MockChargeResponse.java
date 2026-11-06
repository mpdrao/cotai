package dev.cotai.mockgateway.dto;

import dev.cotai.mockgateway.MockPaymentGateway.ChargeStatus;

import java.time.Instant;

public record MockChargeResponse(String providerReference, ChargeStatus status, long amountCents,
                                 Instant expiresAt, String checkoutUrl) {
}
