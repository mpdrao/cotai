package dev.cotai.payment;

import java.time.Instant;
import java.util.UUID;

public interface PaymentGateway {

    String providerName();

    Charge createCharge(ChargeRequest request);

    boolean isSignatureValid(byte[] rawBody, String signatureHeader);

    record ChargeRequest(UUID reservationId, long amountCents, String description, Instant expiresAt) {
    }

    record Charge(String providerReference, String checkoutUrl, String qrCodePayload) {
    }
}
