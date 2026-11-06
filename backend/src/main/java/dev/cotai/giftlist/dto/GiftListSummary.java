package dev.cotai.giftlist.dto;

import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListStatus;

import java.time.LocalDate;
import java.util.UUID;

public record GiftListSummary(UUID id, String title, String slug, GiftListStatus status, LocalDate eventDate) {

    public static GiftListSummary of(GiftList l) {
        return new GiftListSummary(l.getId(), l.getTitle(), l.getSlug(), l.getStatus(), l.getEventDate());
    }
}
