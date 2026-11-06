package dev.cotai.giftlist.dto;

public record ItemTotals(Long itemId, String name, int totalQuotas, int confirmedQuotas, int pendingQuotas,
                         int availableQuotas) {
}
