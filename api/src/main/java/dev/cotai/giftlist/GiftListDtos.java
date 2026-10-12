package dev.cotai.giftlist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request/response records of the organizer and public gift list endpoints. */
public final class GiftListDtos {

    private GiftListDtos() {
    }

    public record ItemInput(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 30) String unit,
            @Min(1) @Max(100_000_000) long unitPriceCents,
            @Min(1) @Max(1000) int totalQuotas) {
    }

    public record CreateGiftListRequest(
            @NotBlank String eventTypeCode,
            @NotBlank @Size(min = 3, max = 80) String title,
            @NotNull LocalDate eventDate,
            @Min(10) @Max(1440) Integer reservationTtlMinutes,
            @NotNull @Size(max = 100) List<@Valid ItemInput> items) {
    }

    public record UpdateGiftListRequest(
            @Size(min = 3, max = 80) String title,
            LocalDate eventDate,
            @Min(10) @Max(1440) Integer reservationTtlMinutes) {
    }

    public record ItemResponse(Long id, String name, String unit, long unitPriceCents, int totalQuotas,
                               long totalCents, boolean archived) {
        static ItemResponse of(Item i) {
            return new ItemResponse(i.getId(), i.getName(), i.getUnit(), i.getUnitPriceCents(),
                    i.getTotalQuotas(), i.totalCents(), i.isArchived());
        }
    }

    public record GiftListResponse(UUID id, String title, String slug, String eventTypeCode, LocalDate eventDate,
                                   GiftListStatus status, int reservationTtlMinutes, List<ItemResponse> items,
                                   Instant createdAt) {
        static GiftListResponse of(GiftList l) {
            return new GiftListResponse(l.getId(), l.getTitle(), l.getSlug(), l.getEventType().getCode(),
                    l.getEventDate(), l.getStatus(), l.getReservationTtlMinutes(),
                    l.getItems().stream().map(ItemResponse::of).toList(), l.getCreatedAt());
        }
    }

    public record GiftListSummary(UUID id, String title, String slug, GiftListStatus status, LocalDate eventDate) {
        static GiftListSummary of(GiftList l) {
            return new GiftListSummary(l.getId(), l.getTitle(), l.getSlug(), l.getStatus(), l.getEventDate());
        }
    }

    public record ItemTotals(Long itemId, String name, int totalQuotas, int confirmedQuotas, int pendingQuotas,
                             int availableQuotas) {
    }

    public record Contributor(String guestName, String itemName, int quantity, long amountCents,
                              Instant confirmedAt) {
    }

    public record GiftListSummaryReport(UUID giftListId, long targetCents, long confirmedCents, long pendingCents,
                                        long refundRequiredCents, List<ItemTotals> items,
                                        List<Contributor> contributors) {
    }

    public record PublicItem(Long id, String name, String unit, long unitPriceCents, int totalQuotas,
                             int availableQuotas) {
    }

    public record PublicGiftList(String title, String eventTypeCode, LocalDate eventDate, GiftListStatus status,
                                 boolean acceptsReservations, List<PublicItem> items) {
    }
}
