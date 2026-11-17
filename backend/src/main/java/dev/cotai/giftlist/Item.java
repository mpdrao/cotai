package dev.cotai.giftlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item")
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gift_list_id", nullable = false)
    private GiftList giftList;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public Item(GiftList giftList, String name, String unit, long unitPriceCents, String imageUrl) {
        this.giftList = giftList;
        this.name = name;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.imageUrl = imageUrl;
        this.isDefault = false;
    }

    Item(GiftList giftList, String name, String unit, long unitPriceCents) {
        this(giftList, name, unit, unitPriceCents, null);
    }

    public long totalCents() {
        return unitPriceCents;
    }

    public void update(String newName, String newUnit, long newUnitPriceCents) {
        this.name = newName;
        this.unit = newUnit;
        this.unitPriceCents = newUnitPriceCents;
    }

    public void archive() {
        this.archived = true;
    }
}
