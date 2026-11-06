package dev.cotai.eventtype.dto;

public record TemplateItemResponse(int position, String name, String unit, long unitPriceCents,
                                   int suggestedQuotas, String imageUrl) {
}
