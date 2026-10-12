package dev.cotai.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Settings under {@code payment.*}. */
@Validated
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        @NotBlank String webhookSecret,
        @Min(100) long gatewayTimeoutMs,
        @Min(1) int maxPendingReservationsPerEmail
) {
}
