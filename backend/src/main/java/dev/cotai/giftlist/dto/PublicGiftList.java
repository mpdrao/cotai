package dev.cotai.giftlist.dto;

import dev.cotai.giftlist.GiftListStatus;

import java.time.LocalDate;
import java.util.List;

public record PublicGiftList(String title, String eventTypeCode, LocalDate eventDate, GiftListStatus status,
                             boolean acceptsReservations, List<PublicItem> items) {
}
