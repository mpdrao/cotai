package dev.cotai.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentInfo(UUID paymentId, PaymentStatus status, long amountCents, String providerReference,
                          String checkoutUrl, String qrCodePayload, Instant expiresAt) {

    public static PaymentInfo of(Payment p) {
        return new PaymentInfo(p.getId(), p.getStatus(), p.getAmountCents(), p.getProviderReference(),
                p.getCheckoutUrl(), p.getQrCodePayload(), p.getExpiresAt());
    }
}
