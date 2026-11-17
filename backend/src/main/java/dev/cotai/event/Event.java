package dev.cotai.event;

import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.Item;
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
 * Event of a gift list (cotai - modelo de objeto).
 *
 * <p>Catalog Events (barbecue, birthday, wedding...) have no parent {@code event}: each one belongs to a
 * template list owned by the system and offers the suggested {@code item}s. When an organizer creates a
 * list (US-07), the list gets its own Event, whose {@code event} points to the catalog Event it was based on
 * and whose {@code code} is the list slug.</p>
 */
@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "default_reservation_ttl_minutes", nullable = false)
    private int defaultReservationTtlMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_code")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gift_list_id", nullable = false)
    private GiftList giftList;

    @OneToMany
    @JoinColumn(name = "event_code")
    @OrderBy("id ASC")
    private List<Item> item = new ArrayList<>();

    public Event(String code, String name, int defaultReservationTtlMinutes, Event event, GiftList giftList) {
        this.code = code;
        this.name = name;
        this.defaultReservationTtlMinutes = defaultReservationTtlMinutes;
        this.event = event;
        this.giftList = giftList;
    }
}
