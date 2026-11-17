package dev.cotai.event;

import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.Item;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * An event from the catalog (barbecue, birthday, wedding...). It brings the
 * suggested items and the default payment window used when a list is created.
 */
@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "default_reservation_ttl_minutes", nullable = false)
    private int defaultReservationTtlMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_code")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_list_id")
    private GiftList giftList;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Item> items = new ArrayList<>();

    public Event(String code, String name, int defaultReservationTtlMinutes) {
        this.code = code;
        this.name = name;
        this.defaultReservationTtlMinutes = defaultReservationTtlMinutes;
    }

    public Event(String code, String name, int defaultReservationTtlMinutes, Event event) {
        this(code, name, defaultReservationTtlMinutes);
        this.event = event;
    }

    /** Items offered when an organizer picks this event, in catalog order. */
    public List<Item> suggestedItems() {
        return items.stream()
                .filter(Item::isDefault)
                .filter(item -> !item.isArchived())
                .toList();
    }
}
