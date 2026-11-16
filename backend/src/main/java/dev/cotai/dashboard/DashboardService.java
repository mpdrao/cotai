package dev.cotai.dashboard;

import dev.cotai.common.BusinessException;
import dev.cotai.dashboard.DashboardDtos.Contribution;
import dev.cotai.dashboard.DashboardDtos.ContributionPage;
import dev.cotai.dashboard.DashboardDtos.ContributionTotals;
import dev.cotai.dashboard.DashboardDtos.FundraisingMetrics;
import dev.cotai.dashboard.DashboardDtos.ItemAggregate;
import dev.cotai.dashboard.DashboardDtos.ItemProgress;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListRepository;
import dev.cotai.giftlist.Item;
import dev.cotai.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    static final int QUOTAS_PER_ITEM = 1;
    static final int MAX_PAGE_SIZE = 50;

    private final GiftListRepository giftLists;
    private final DashboardRepository dashboard;
    private final PaymentRepository payments;
    private final Clock clock;

    public FundraisingMetrics metrics(UUID ownerId, UUID giftListId) {
        GiftList list = owned(ownerId, giftListId);
        Map<Long, ItemAggregate> byItem = dashboard.aggregateByItem(list.getId(), clock.instant()).stream()
                .collect(Collectors.toMap(ItemAggregate::itemId, Function.identity()));

        List<Item> active = list.getItems().stream().filter(i -> !i.isArchived()).toList();
        List<ItemProgress> items = active.stream().map(i -> progress(i, byItem.get(i.getId()))).toList();

        long target = active.stream().mapToLong(Item::totalCents).sum();
        long confirmed = items.stream().mapToLong(ItemProgress::paidCents).sum();
        long pending = active.stream()
                .map(i -> byItem.get(i.getId()))
                .filter(Objects::nonNull)
                .mapToLong(ItemAggregate::pendingCents).sum();
        int paid = items.stream().mapToInt(ItemProgress::paidQuotas).sum();
        int pendingQuotas = items.stream().mapToInt(ItemProgress::pendingQuotas).sum();
        int remaining = items.stream().mapToInt(ItemProgress::remainingQuotas).sum();
        ContributionTotals totals = dashboard.contributionTotals(list.getId());

        return new FundraisingMetrics(list.getId(), list.getTitle(), list.getStatus(), target, confirmed, pending,
                payments.sumLatePayments(list.getId()), percent(confirmed, target), items.size() * QUOTAS_PER_ITEM, paid, pendingQuotas, remaining,
                totals.contributions(), totals.contributors(), totals.lastPaidAt(), items);
    }

    public List<Contribution> allContributions(UUID ownerId, UUID giftListId) {
        return dashboard.findAllContributions(owned(ownerId, giftListId).getId());
    }

    public ContributionPage contributions(UUID ownerId, UUID giftListId, Long itemId, int page, int size) {
        GiftList list = owned(ownerId, giftListId);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<Contribution> result;
        if (itemId == null) {
            result = dashboard.findContributions(list.getId(), pageable);
        } else {
            if (list.getItems().stream().noneMatch(i -> itemId.equals(i.getId()))) {
                throw BusinessException.notFound("Item");
            }
            result = dashboard.findContributionsByItem(list.getId(), itemId, pageable);
        }
        return new ContributionPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private static ItemProgress progress(Item item, ItemAggregate a) {
        int paid = a == null ? 0 : (int) a.paidQuotas();
        int pending = a == null ? 0 : (int) a.pendingQuotas();
        long paidCents = a == null ? 0 : a.paidCents();
        return new ItemProgress(item.getId(), item.getName(), item.getUnitPriceCents(), QUOTAS_PER_ITEM, paid,
                pending, Math.max(QUOTAS_PER_ITEM - paid - pending, 0), paidCents);
    }

    private static int percent(long confirmedCents, long targetCents) {
        if (targetCents <= 0) {
            return 0;
        }
        return (int) Math.min(100, confirmedCents * 100 / targetCents);
    }

    private GiftList owned(UUID ownerId, UUID id) {
        return giftLists.findWithItemsById(id)
                .filter(l -> l.isOwnedBy(ownerId))
                .orElseThrow(() -> BusinessException.notFound("Gift list"));
    }
}
