package dev.cotai.giftlist;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.eventtype.EventType;
import dev.cotai.organizer.Organizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gift list of an organizer. Named {@code GiftList} (not {@code List}) to avoid clashing with
 * {@link java.util.List}.
 */
@Entity
@Table(name = "gift_list")
public class GiftList {

    public static final int MAX_ITEMS = 100;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private Organizer owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_type_code")
    private EventType eventType;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GiftListStatus status;

    /** 10..1440. Applies to new reservations only. */
    @Column(name = "reservation_ttl_minutes", nullable = false)
    private int reservationTtlMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "giftList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Item> items = new ArrayList<>();

    protected GiftList() {
    }

    public GiftList(Organizer owner, EventType eventType, String title, String slug, LocalDate eventDate,
                    int reservationTtlMinutes, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.eventType = eventType;
        this.title = title;
        this.slug = slug;
        this.eventDate = eventDate;
        this.reservationTtlMinutes = reservationTtlMinutes;
        this.createdAt = createdAt;
        this.status = GiftListStatus.DRAFT;
    }

    public Item addItem(String name, String unit, long unitPriceCents, int totalQuotas) {
        ensureNotClosed();
        if (items.size() >= MAX_ITEMS) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "A gift list has at most " + MAX_ITEMS + " items.");
        }
        Item item = new Item(this, name, unit, unitPriceCents, totalQuotas);
        items.add(item);
        return item;
    }

    /** UC04. DRAFT -> PUBLISHED. */
    public void publish(LocalDate today) {
        if (status != GiftListStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "Only DRAFT lists can be published.");
        }
        if (eventDate.isBefore(today)) {
            throw new BusinessException(ErrorCode.EVENT_DATE_IN_PAST, "Event date is in the past.");
        }
        if (items.stream().allMatch(Item::isArchived)) {
            throw new BusinessException(ErrorCode.LIST_HAS_NO_ITEMS, "Add at least one item before publishing.");
        }
        status = GiftListStatus.PUBLISHED;
    }

    /** UC07. PUBLISHED -> CLOSED. PENDING reservations can still be paid until they expire. */
    public void close() {
        if (status != GiftListStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "Only PUBLISHED lists can be closed.");
        }
        status = GiftListStatus.CLOSED;
    }

    /** {@code today} must be computed in {@code app.zone}, never in UTC (EC24). */
    public boolean acceptsReservations(LocalDate today) {
        return status == GiftListStatus.PUBLISHED && !eventDate.isBefore(today);
    }

    public void update(String newTitle, LocalDate newEventDate, Integer newTtlMinutes) {
        ensureNotClosed();
        if (newTitle != null) {
            this.title = newTitle;
        }
        if (newEventDate != null) {
            this.eventDate = newEventDate;
        }
        if (newTtlMinutes != null) {
            this.reservationTtlMinutes = newTtlMinutes;
        }
    }

    public void ensureNotClosed() {
        if (status == GiftListStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "Gift list is CLOSED.");
        }
    }

    public boolean isOwnedBy(UUID organizerId) {
        return owner.getId().equals(organizerId);
    }

    public UUID getId() {
        return id;
    }

    public Organizer getOwner() {
        return owner;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public GiftListStatus getStatus() {
        return status;
    }

    public int getReservationTtlMinutes() {
        return reservationTtlMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Item> getItems() {
        return List.copyOf(items);
    }
}
