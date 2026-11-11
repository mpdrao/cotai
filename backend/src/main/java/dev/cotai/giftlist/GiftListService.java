package dev.cotai.giftlist;

import dev.cotai.common.AppProperties;
import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.eventtype.EventType;
import dev.cotai.eventtype.EventTypeRepository;
import dev.cotai.giftlist.GiftListDtos.Contributor;
import dev.cotai.giftlist.GiftListDtos.CreateGiftListRequest;
import dev.cotai.giftlist.GiftListDtos.GiftListResponse;
import dev.cotai.giftlist.GiftListDtos.GiftListSummary;
import dev.cotai.giftlist.GiftListDtos.GiftListSummaryReport;
import dev.cotai.giftlist.GiftListDtos.ItemInput;
import dev.cotai.giftlist.GiftListDtos.ItemResponse;
import dev.cotai.giftlist.GiftListDtos.ItemTotals;
import dev.cotai.giftlist.GiftListDtos.PublicGiftList;
import dev.cotai.giftlist.GiftListDtos.PublicItem;
import dev.cotai.giftlist.GiftListDtos.UpdateGiftListRequest;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.payment.PaymentRepository;
import dev.cotai.reservation.Reservation;
import dev.cotai.reservation.ReservationRepository;
import dev.cotai.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class GiftListService {

    private final GiftListRepository giftLists;
    private final ItemRepository items;
    private final EventTypeRepository eventTypes;
    private final OrganizerRepository organizers;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final SlugGenerator slugGenerator;
    private final AppProperties props;
    private final Clock clock;

    public GiftListResponse create(UUID ownerId, CreateGiftListRequest req) {
        EventType eventType = eventTypes.findById(req.eventTypeCode())
                .orElseThrow(() -> BusinessException.notFound("Event type"));
        if (req.eventDate().isBefore(today())) {
            throw new BusinessException(ErrorCode.EVENT_DATE_IN_PAST, "Event date is in the past.");
        }
        int ttl = req.reservationTtlMinutes() != null
                ? req.reservationTtlMinutes() : eventType.getDefaultReservationTtlMinutes();

        GiftList list = new GiftList(organizers.getReferenceById(ownerId), eventType, req.title().trim(),
                uniqueSlug(req.title()), req.eventDate(), ttl, clock.instant());
        for (ItemInput in : req.items()) {
            Item item = list.addItem(in.name().trim(), in.unit().trim(), in.unitPriceCents());
            item.setImageUrl(in.imageUrl());
        }
        return GiftListResponse.of(giftLists.save(list));
    }

    @Transactional(readOnly = true)
    public List<GiftListSummary> listMine(UUID ownerId) {
        return giftLists.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(GiftListSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public GiftListResponse get(UUID ownerId, UUID id) {
        return GiftListResponse.of(owned(ownerId, id));
    }

    public GiftListResponse update(UUID ownerId, UUID id, UpdateGiftListRequest req) {
        GiftList list = owned(ownerId, id);
        list.update(req.title(), req.eventDate(), req.reservationTtlMinutes());
        return GiftListResponse.of(list);
    }

    public GiftListResponse publish(UUID ownerId, UUID id) {
        GiftList list = owned(ownerId, id);
        list.publish(today());
        return GiftListResponse.of(list);
    }

    public GiftListResponse close(UUID ownerId, UUID id) {
        GiftList list = owned(ownerId, id);
        list.close();
        return GiftListResponse.of(list);
    }

    public ItemResponse addItem(UUID ownerId, UUID id, ItemInput in) {
        GiftList list = owned(ownerId, id);
        Item item = list.addItem(in.name().trim(), in.unit().trim(), in.unitPriceCents());
        item.setImageUrl(in.imageUrl());
        giftLists.flush();
        return ItemResponse.of(item);
    }

    public ItemResponse updateItem(UUID ownerId, UUID id, Long itemId, ItemInput in) {
        GiftList list = owned(ownerId, id);
        list.ensureNotClosed();
        Item item = lockedItemOf(list, itemId);
        item.update(in.name().trim(), in.unit().trim(), in.unitPriceCents());
        if (in.imageUrl() != null) {
            item.setImageUrl(in.imageUrl());
        }
        return ItemResponse.of(item);
    }

    public void archiveItem(UUID ownerId, UUID id, Long itemId) {
        GiftList list = owned(ownerId, id);
        list.ensureNotClosed();
        Item item = lockedItemOf(list, itemId);
        if (reservations.hasActiveReservations(itemId, clock.instant())) {
            throw new BusinessException(ErrorCode.ITEM_HAS_ACTIVE_RESERVATIONS,
                    "Item has CONFIRMED or active PENDING reservations.");
        }
        item.archive();
    }

    public ItemResponse uploadPhoto(UUID ownerId, UUID id, Long itemId, byte[] data) {
        Item item = photoTarget(ownerId, id, itemId);
        String base64 = Base64.getEncoder().encodeToString(data);
        item.setImageUrl("data:image/jpeg;base64," + base64);
        return ItemResponse.of(item);
    }

    public ItemResponse removePhoto(UUID ownerId, UUID id, Long itemId) {
        Item item = photoTarget(ownerId, id, itemId);
        item.setImageUrl(null);
        return ItemResponse.of(item);
    }

    private Item photoTarget(UUID ownerId, UUID id, Long itemId) {
        GiftList list = owned(ownerId, id);
        list.ensureNotClosed();
        Item item = lockedItemOf(list, itemId);
        if (item.isArchived()) {
            throw BusinessException.notFound("Item");
        }
        return item;
    }

    @Transactional(readOnly = true)
    public GiftListSummaryReport summary(UUID ownerId, UUID id) {
        GiftList list = owned(ownerId, id);
        Instant now = clock.instant();
        List<Reservation> all = reservations.findAllByGiftListId(list.getId());

        Map<Long, int[]> perItem = new HashMap<>(); 
        long confirmedCents = 0;
        long pendingCents = 0;
        for (Reservation r : all) {
            int[] counters = perItem.computeIfAbsent(r.getItem().getId(), k -> new int[2]);
            if (r.getStatus() == ReservationStatus.CONFIRMED) {
                counters[0] += r.getQuantity();
                confirmedCents += r.getAmountCents();
            } else if (r.effectiveStatus(now) == ReservationStatus.PENDING) {
                counters[1] += r.getQuantity();
                pendingCents += r.getAmountCents();
            }
        }

        List<Item> active = list.getItems().stream().filter(i -> !i.isArchived()).toList();
        List<ItemTotals> totals = active.stream().map(i -> {
            int[] c = perItem.getOrDefault(i.getId(), new int[2]);
            return new ItemTotals(i.getId(), i.getName(), 1, c[0], c[1],
                    Math.max(1 - c[0] - c[1], 0));
        }).toList();

        List<Contributor> contributors = all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .sorted(Comparator.comparing(Reservation::getClosedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(r -> new Contributor(r.getGuestName(), r.getItem().getName(), r.getQuantity(),
                        r.getAmountCents(), r.getClosedAt()))
                .toList();

        long target = active.stream().mapToLong(Item::totalCents).sum();
        return new GiftListSummaryReport(list.getId(), target, confirmedCents, pendingCents,
                payments.sumRefundRequired(list.getId()), totals, contributors);
    }

    @Transactional(readOnly = true)
    public PublicGiftList publicView(String slug) {
        GiftList list = giftLists.findBySlug(slug)
                .filter(l -> l.getStatus() != GiftListStatus.DRAFT)
                .orElseThrow(() -> BusinessException.notFound("Gift list"));
        Instant now = clock.instant();
        Map<Long, Integer> occupied = new HashMap<>();
        for (Object[] row : items.occupiedQuotasByItem(list.getId(), now)) {
            occupied.put((Long) row[0], ((Number) row[1]).intValue());
        }
        List<PublicItem> publicItems = list.getItems().stream()
                .filter(i -> !i.isArchived())
                .map(i -> new PublicItem(i.getId(), i.getName(), i.getUnit(), i.getUnitPriceCents(),
                        1, Math.max(1 - occupied.getOrDefault(i.getId(), 0), 0),
                        i.getImageUrl()))
                .toList();
        return new PublicGiftList(list.getTitle(), list.getEventType().getCode(), list.getEventDate(),
                list.getStatus(), list.acceptsReservations(today()), publicItems);
    }

    private GiftList owned(UUID ownerId, UUID id) {
        return giftLists.findWithItemsById(id)
                .filter(l -> l.isOwnedBy(ownerId))
                .orElseThrow(() -> BusinessException.notFound("Gift list"));
    }

    private Item lockedItemOf(GiftList list, Long itemId) {
        return items.lockById(itemId)
                .filter(i -> i.getGiftList().getId().equals(list.getId()))
                .orElseThrow(() -> BusinessException.notFound("Item"));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), props.zone());
    }

    private String uniqueSlug(String title) {
        String slug;
        do {
            slug = slugGenerator.generate(title);
        } while (giftLists.existsBySlug(slug));
        return slug;
    }
}
