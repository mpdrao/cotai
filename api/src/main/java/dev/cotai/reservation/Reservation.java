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

import java.time.Instant;
import java.util.UUID;

/**
 * Quotas reserved by a guest. State transitions are conditional UPDATEs in
 * {@link ReservationRepository} ({@code WHERE status = 'PENDING'}), never setters, so concurrent
 * transitions (pay vs expire vs cancel) have exactly one winner.
 */
@Entity
@Table(name = "reservation")
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

    /** Snapshot of the item price at creation (EC18). */
    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    private long unitPriceCents;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /** SHA-256 of the manage token. The token itself is never stored. */
    @Column(name = "manage_token_hash", nullable = false, length = 64, updatable = false)
    private String manageTokenHash;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    /** SHA-256 of the request body, to detect a reused Idempotency-Key with another body (EC05). */
    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Reservation() {
    }

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

    /** A PENDING reservation past its deadline is EXPIRED even before the job runs (EC15). */
    public ReservationStatus effectiveStatus(Instant now) {
        if (status == ReservationStatus.PENDING && !now.isBefore(expiresAt)) {
            return ReservationStatus.EXPIRED;
        }
        return status;
    }

    public UUID getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getManageTokenHash() {
        return manageTokenHash;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
