package dev.cotai.giftlist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ItemInput(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 30) String unit,
        @Min(1) @Max(100_000_000) long unitPriceCents,
        @Min(1) @Max(1000) int totalQuotas,
        @Pattern(regexp = "^/suggestions/[a-z0-9-]+\\.jpg$") String imageUrl) {
}
