package dev.cotai.giftlist.dto;

import dev.cotai.giftlist.Item;

public record ItemResponse(Long id, String name, String unit, long unitPriceCents, int totalQuotas,
                           long totalCents, boolean archived, String imageUrl) {

    public static ItemResponse of(Item i) {
        return new ItemResponse(i.getId(), i.getName(), i.getUnit(), i.getUnitPriceCents(),
                i.getTotalQuotas(), i.totalCents(), i.isArchived(), i.getImageUrl());
    }
}
