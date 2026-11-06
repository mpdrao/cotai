package dev.cotai.giftlist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateGiftListRequest(
        @NotBlank String eventTypeCode,
        @NotBlank @Size(min = 3, max = 80) String title,
        @NotNull LocalDate eventDate,
        @Min(10) @Max(1440) Integer reservationTtlMinutes,
        @NotNull @Size(max = 100) List<@Valid ItemInput> items) {
}
