package dev.cotai.dashboard;

import dev.cotai.giftlist.GiftListStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Per-item sums computed by the database; unpaid and expired reservations are never counted. */
    public record ItemAggregate(Long itemId, long paidQuotas, long pendingQuotas, long paidCents, long pendingCents) {
    }

    public record ContributionTotals(long contributions, long contributors, Instant lastPaidAt) {
    }

    public record ItemProgress(Long itemId, String name, long unitPriceCents, int totalQuotas, int paidQuotas,
                               int pendingQuotas, int remainingQuotas, long paidCents) {
    }

    public record FundraisingMetrics(UUID giftListId, String title, GiftListStatus status, long targetCents,
                                     long confirmedCents, long pendingCents, long latePaymentsCents, int percentRaised, int totalQuotas,
                                     int paidQuotas, int pendingQuotas, int remainingQuotas, long contributions,
                                     long contributors, Instant lastContributionAt, List<ItemProgress> items) {
    }

    public record Contribution(UUID reservationId, String guestName, Long itemId, String itemName, int quantity,
                               long amountCents, Instant paidAt, String message) {
    }

    public record ContributionPage(List<Contribution> content, int page, int size, long totalElements,
                                   int totalPages) {
    }
}
