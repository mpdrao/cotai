package dev.cotai.payment;

import dev.cotai.payment.dto.Charge;
import dev.cotai.payment.dto.ChargeRequest;

public interface PaymentGateway {

    String providerName();

    Charge createCharge(ChargeRequest request);

    boolean isSignatureValid(byte[] rawBody, String signatureHeader);
}
