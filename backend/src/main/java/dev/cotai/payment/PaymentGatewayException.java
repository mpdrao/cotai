package dev.cotai.payment;

/** The payment provider failed or timed out while creating a charge (EC07). */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentGatewayException(String message) {
        super(message);
    }
}
