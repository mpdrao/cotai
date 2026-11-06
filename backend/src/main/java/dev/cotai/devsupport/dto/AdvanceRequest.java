package dev.cotai.devsupport.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdvanceRequest(@Min(1) @Max(100000) int minutes) {
}
