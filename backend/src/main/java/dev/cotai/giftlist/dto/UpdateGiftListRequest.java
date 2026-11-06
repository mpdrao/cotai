package dev.cotai.giftlist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateGiftListRequest(
        @Size(min = 3, max = 80) String title,
        LocalDate eventDate,
        @Min(10) @Max(1440) Integer reservationTtlMinutes) {
}
