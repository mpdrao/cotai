package dev.cotai.eventtype;

import dev.cotai.giftlist.Item;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A type of event from the catalog (barbecue, birthday, wedding...). It brings the
 * suggested items and the default payment window used when a list is created.
 * The catalog is loaded by the Flyway migrations and is read-only for the app.
 */
@Entity
@Table(name = "event_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "default_reservation_ttl_minutes", nullable = false)
    private int defaultReservationTtlMinutes;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Getter(AccessLevel.NONE)
    private List<Item> items = new ArrayList<>();

    public Event(String code, String name, int defaultReservationTtlMinutes) {
        this.code = code;
        this.name = name;
        this.defaultReservationTtlMinutes = defaultReservationTtlMinutes;
    }

    /** Items offered when an organizer picks this type, in catalog order. */
    public List<Item> suggestedItems() {
        return items.stream()
                .filter(Item::isDefault)
                .filter(item -> !item.isArchived())
                .toList();
    }
}
