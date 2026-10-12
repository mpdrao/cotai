package dev.cotai.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import dev.cotai.reservation.Reservation;

import java.time.Instant;
import java.util.UUID;

/**
 * One charge per reservation (1:1). {@code amountCents} and {@code expiresAt} are copied from the
 * reservation in the same transaction. Status changes are conditional UPDATEs in
 * {@link PaymentRepository#transition}.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", updatable = false)
    private Reservation reservation;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_reference", length = 80)
    private String providerReference;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "qr_code_payload", length = 500)
    private String qrCodePayload;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Payment() {
    }

    public Payment(Reservation reservation, String provider) {
        this.id = UUID.randomUUID();
        this.reservation = reservation;
        this.provider = provider;
        this.amountCents = reservation.getAmountCents();
        this.expiresAt = reservation.getExpiresAt();
        this.status = PaymentStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getQrCodePayload() {
        return qrCodePayload;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
