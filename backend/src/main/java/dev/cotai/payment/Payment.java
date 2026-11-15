package dev.cotai.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    private UUID id;

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

    public Payment(String provider, long amountCents, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.provider = provider;
        this.amountCents = amountCents;
        this.expiresAt = expiresAt;
        this.status = PaymentStatus.CREATED;
    }

}
