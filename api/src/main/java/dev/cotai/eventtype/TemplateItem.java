package dev.cotai.eventtype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Suggested item of an event type. Copied (never referenced) when a gift list is created. */
@Entity
@Table(name = "template_item")
public class TemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_type_code")
    private EventType eventType;

    /** 1..10, unique per event type. */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(name = "suggested_quotas", nullable = false)
    private int suggestedQuotas;

    protected TemplateItem() {
    }

    public Long getId() {
        return id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public int getSuggestedQuotas() {
        return suggestedQuotas;
    }
}
