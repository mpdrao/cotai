package dev.cotai.payment;

import dev.cotai.payment.dto.Charge;
import dev.cotai.payment.dto.ChargeRequest;

/**
 * Port to the payment provider. The only implementation today is {@code MockPaymentGateway}
 * ({@code dev} profile); a real provider (Pix PSP, Stripe, ...) just implements this interface.
 */
public interface PaymentGateway {

    /** Stored in {@code payment.provider}. */
    String providerName();

    /**
     * Opens a charge. Called OUTSIDE any database transaction/row lock.
     *
     * @throws PaymentGatewayException on provider error or timeout
     */
    Charge createCharge(ChargeRequest request);

    /** Verifies the signature of the webhook over the RAW body bytes (never a re-serialized DTO). */
    boolean isSignatureValid(byte[] rawBody, String signatureHeader);
}
