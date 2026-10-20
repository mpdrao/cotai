package dev.cotai.giftlist;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
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
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Item of a gift list, split into {@code totalQuotas} quotas of {@code unitPriceCents} each.
 * The available quotas are NOT persisted: they are derived from reservations under a row lock
 * (see {@link ItemRepository#lockById(Long)} and {@link ItemRepository#occupiedQuotas}).
 */
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
    @JoinColumn(name = "gift_list_id")
    private GiftList giftList;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(name = "total_quotas", nullable = false)
    private int totalQuotas;

    @Column(nullable = false)
    private boolean archived;

    Item(GiftList giftList, String name, String unit, long unitPriceCents, int totalQuotas) {
        this.giftList = giftList;
        this.name = name;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.totalQuotas = totalQuotas;
    }

    public long totalCents() {
        return unitPriceCents * totalQuotas;
    }

    /** Price changes never affect existing reservations: they keep a price snapshot (EC18). */
    public void update(String newName, String newUnit, long newUnitPriceCents) {
        this.name = newName;
        this.unit = newUnit;
        this.unitPriceCents = newUnitPriceCents;
    }

    /** Must be called while holding the item row lock, with {@code occupied} computed under it. */
    public void changeTotalQuotas(int newTotal, int occupied) {
        if (newTotal < occupied) {
            throw new BusinessException(ErrorCode.QUOTAS_BELOW_OCCUPIED,
                    "Item " + id + " has " + occupied + " occupied quotas.", Map.of("occupiedQuotas", occupied));
        }
        this.totalQuotas = newTotal;
    }

    public void archive() {
        this.archived = true;
    }

}
