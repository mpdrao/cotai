package dev.cotai.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReserveRequest(
        @NotBlank @Size(min = 2, max = 80) String guestName,
        @NotBlank @Email @Size(max = 254) String guestEmail,
        @Min(1) @Max(1000) int quantity) {
}
