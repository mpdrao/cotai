package dev.cotai.eventtype;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** Event type (WEDDING, BABY_SHOWER, BARBECUE, OTHER) with up to 10 suggested items. Seeded by Flyway V2. */
@Entity
@Table(name = "event_type")
public class EventType {

    @Id
    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "default_reservation_ttl_minutes", nullable = false)
    private int defaultReservationTtlMinutes;

    @OneToMany(mappedBy = "eventType", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TemplateItem> templateItems = new ArrayList<>();

    protected EventType() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getDefaultReservationTtlMinutes() {
        return defaultReservationTtlMinutes;
    }

    public List<TemplateItem> getTemplateItems() {
        return List.copyOf(templateItems);
    }
}
