package dev.cotai.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

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
