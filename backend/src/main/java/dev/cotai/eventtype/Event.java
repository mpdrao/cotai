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
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_type")
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

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<Item> items = new ArrayList<>();

    public Event(String code, String name, int defaultReservationTtlMinutes) {
        this.code = code;
        this.name = name;
        this.defaultReservationTtlMinutes = defaultReservationTtlMinutes;
    }

    public List<Item> getItems() {
        return List.copyOf(items);
    }
}
