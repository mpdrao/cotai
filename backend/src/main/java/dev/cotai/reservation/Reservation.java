package dev.cotai.reservation;

import dev.cotai.giftlist.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

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

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public Reservation(UUID id, Item item, String guestName, String guestEmail, int quantity,
                       String manageTokenHash, UUID idempotencyKey, String requestHash,
                       Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.item = item;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.quantity = quantity;
        this.unitPriceCents = item.getUnitPriceCents();
        this.amountCents = Math.multiplyExact(quantity, unitPriceCents);
        this.status = ReservationStatus.PENDING;
        this.manageTokenHash = manageTokenHash;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public ReservationStatus effectiveStatus(Instant now) {
        if (status == ReservationStatus.PENDING && !now.isBefore(expiresAt)) {
            return ReservationStatus.EXPIRED;
        }
        return status;
    }

}
