package dev.cotai.reservation;

import dev.cotai.giftlist.Item;
import dev.cotai.payment.Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", updatable = false)
    private Item item;

    @Column(name = "guest_name", nullable = false, length = 80)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 254)
    private String guestEmail;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    private long unitPriceCents;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "manage_token_hash", nullable = false, length = 64, updatable = false)
    private String manageTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private Payment payment;

    public Reservation(UUID id, Item item, String guestName, String guestEmail, int quantity,
                       String manageTokenHash, Instant createdAt) {
        this.id = id;
        this.item = item;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.quantity = quantity;
        this.unitPriceCents = item.getUnitPriceCents();
        this.amountCents = Math.multiplyExact(quantity, unitPriceCents);
        this.status = ReservationStatus.PENDING;
        this.manageTokenHash = manageTokenHash;
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        int ttlMinutes = item != null && item.getGiftList() != null ? item.getGiftList().getReservationTtlMinutes() : 60;
        return createdAt.plus(java.time.Duration.ofMinutes(ttlMinutes));
    }

    public ReservationStatus effectiveStatus(Instant now) {
        if (status == ReservationStatus.PENDING && !now.isBefore(getExpiresAt())) {
            return ReservationStatus.EXPIRED;
        }
        return status;
    }
}
