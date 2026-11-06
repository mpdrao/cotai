package dev.cotai.giftlist.dto;

import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GiftListResponse(UUID id, String title, String slug, String eventTypeCode, LocalDate eventDate,
                               GiftListStatus status, int reservationTtlMinutes, List<ItemResponse> items,
                               Instant createdAt) {

    public static GiftListResponse of(GiftList l) {
        return new GiftListResponse(l.getId(), l.getTitle(), l.getSlug(), l.getEventType().getCode(),
                l.getEventDate(), l.getStatus(), l.getReservationTtlMinutes(),
                l.getItems().stream().map(ItemResponse::of).toList(), l.getCreatedAt());
    }
}
