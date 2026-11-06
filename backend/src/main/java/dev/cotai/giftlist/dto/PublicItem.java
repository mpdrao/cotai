package dev.cotai.giftlist.dto;

public record PublicItem(Long id, String name, String unit, long unitPriceCents, int totalQuotas,
                         int availableQuotas, String imageUrl) {
}
