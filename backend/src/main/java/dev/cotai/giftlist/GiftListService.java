package dev.cotai.giftlist;

import dev.cotai.security.AppProperties;
import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.dashboard.DashboardDtos.FundraisingMetrics;
import dev.cotai.dashboard.DashboardService;
import dev.cotai.event.Event;
import dev.cotai.event.EventRepository;
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
import dev.cotai.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
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
    private final EventRepository events;
    private final OrganizerRepository organizers;
    private final ReservationRepository reservations;
    private final DashboardService dashboard;
    private final SlugGenerator slugGenerator;
    private final AppProperties props;
    private final Clock clock;

    /** US-07: the list gets its own Event, based on the catalog Event the organizer picked. */
    public GiftListResponse create(UUID ownerId, CreateGiftListRequest req) {
        Event catalogEvent = catalogEvent(req.eventCode());
        ensureNotInThePast(req.eventDate());
        int ttl = req.reservationTtlMinutes() != null
                ? req.reservationTtlMinutes() : catalogEvent.getDefaultReservationTtlMinutes();

        GiftList list = new GiftList(organizers.getReferenceById(ownerId), req.title().trim(),
                uniqueSlug(req.title()), req.eventDate(), ttl, clock.instant());
        for (ItemInput in : req.items()) {
            Item item = list.addItem(in.name().trim(), in.unit().trim(), in.unitPriceCents());
            item.setImageUrl(in.imageUrl());
        }
        // The id is assigned in the constructor, so save() merges: the item ids are on the returned copy.
        list = giftLists.save(list);
        events.save(new Event(list.getSlug(), catalogEvent.getName(),
                catalogEvent.getDefaultReservationTtlMinutes(), catalogEvent, list));
        return GiftListResponse.of(list, catalogEvent.getCode());
    }

    @Transactional(readOnly = true)
    public List<GiftListSummary> listMine(UUID ownerId) {
        return giftLists.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(GiftListSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public GiftListResponse get(UUID ownerId, UUID id) {
        return response(owned(ownerId, id));
    }

    public GiftListResponse update(UUID ownerId, UUID id, UpdateGiftListRequest req) {
        GiftList list = owned(ownerId, id);
        // Check everything before touching the list, so a bad field does not leave half an update behind.
        Event newCatalogEvent = req.eventCode() != null ? catalogEvent(req.eventCode()) : null;
        // Only a new date is checked: renaming a list whose event already happened must still work.
        if (req.eventDate() != null && !req.eventDate().equals(list.getEventDate())) {
            ensureNotInThePast(req.eventDate());
        }
        list.update(req.title(), req.eventDate(), req.reservationTtlMinutes());
        Event listEvent = eventOf(list);
        if (newCatalogEvent != null) {
            // Changing the event keeps the items: suggestions are only used when the list is created.
            listEvent.setEvent(newCatalogEvent);
            listEvent.setName(newCatalogEvent.getName());
            listEvent.setDefaultReservationTtlMinutes(newCatalogEvent.getDefaultReservationTtlMinutes());
        }
        return GiftListResponse.of(list, listEvent.getEvent().getCode());
    }

    private Event catalogEvent(String code) {
        return events.findCatalogEvent(code).orElseThrow(() -> BusinessException.notFound("Event"));
    }

    /** The list's own Event (Event - giftList 1). */
    private Event eventOf(GiftList list) {
        return events.findByGiftListId(list.getId()).orElseThrow(() -> BusinessException.notFound("Event"));
    }

    private GiftListResponse response(GiftList list) {
        return GiftListResponse.of(list, eventOf(list).getEvent().getCode());
    }

    private void ensureNotInThePast(LocalDate eventDate) {
        if (eventDate.isBefore(today())) {
            throw new BusinessException(ErrorCode.EVENT_DATE_IN_PAST, "Event date is in the past.");
        }
    }

    public GiftListResponse publish(UUID ownerId, UUID id) {
        GiftList list = owned(ownerId, id);
        list.publish(today());
        return response(list);
    }

    public GiftListResponse close(UUID ownerId, UUID id) {
        GiftList list = owned(ownerId, id);
        list.close();
        return response(list);
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

    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;

    public ItemResponse uploadPhoto(UUID ownerId, UUID id, Long itemId, byte[] data) {
        String contentType = photoContentType(data);
        Item item = photoTarget(ownerId, id, itemId);
        String base64 = Base64.getEncoder().encodeToString(data);
        item.setImageUrl("data:" + contentType + ";base64," + base64);
        return ItemResponse.of(item);
    }

    private static String photoContentType(byte[] data) {
        if (data.length > MAX_PHOTO_BYTES) {
            throw new BusinessException(ErrorCode.PHOTO_TOO_LARGE, "Photo is larger than 5 MB.");
        }
        if (startsWith(data, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "image/jpeg";
        }
        if (startsWith(data, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "image/png";
        }
        if (startsWith(data, 0, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && startsWith(data, 8, "WEBP".getBytes(StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        throw new BusinessException(ErrorCode.PHOTO_INVALID, "Photo must be a JPEG, PNG or WebP image.");
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
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
        FundraisingMetrics metrics = dashboard.metrics(ownerId, id);
        List<ItemTotals> totals = metrics.items().stream()
                .map(i -> new ItemTotals(i.itemId(), i.name(), i.totalQuotas(), i.paidQuotas(), i.pendingQuotas(),
                        i.remainingQuotas()))
                .toList();
        List<Contributor> contributors = dashboard.allContributions(ownerId, id).stream()
                .map(c -> new Contributor(c.guestName(), c.itemName(), c.quantity(), c.amountCents(), c.paidAt()))
                .toList();
        return new GiftListSummaryReport(metrics.giftListId(), metrics.targetCents(), metrics.confirmedCents(),
                metrics.pendingCents(), metrics.latePaymentsCents(), totals, contributors);
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
        return new PublicGiftList(list.getTitle(), eventOf(list).getEvent().getCode(), list.getEventDate(),
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
