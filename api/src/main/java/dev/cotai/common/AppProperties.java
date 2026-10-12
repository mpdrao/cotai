package dev.cotai.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Application settings (prefix {@code app}).
 *
 * @param zone          zone used to compute "today" for event dates (EC24). Default America/Sao_Paulo.
 * @param publicBaseUrl base URL encoded in checkout links / QR codes. Use the LAN IP, not localhost,
 *                      if a phone will scan the QR code (EC27).
 * @param jwt           JWT settings for organizers.
 * @param manageTokenSecret secret used to derive guest manage tokens (HMAC). Never log it.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull ZoneId zone,
        @NotBlank String publicBaseUrl,
        @NotNull Jwt jwt,
        @NotBlank String manageTokenSecret
) {

    public record Jwt(@NotBlank String secret, long ttlSeconds) {
    }
}
