package dev.cotai.giftlist.dto;

import java.util.List;
import java.util.UUID;

public record GiftListSummaryReport(UUID giftListId, long targetCents, long confirmedCents, long pendingCents,
                                    long refundRequiredCents, List<ItemTotals> items,
                                    List<Contributor> contributors) {
}
