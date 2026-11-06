package dev.cotai.giftlist.dto;

import java.time.Instant;

public record Contributor(String guestName, String itemName, int quantity, long amountCents,
                          Instant confirmedAt) {
}
