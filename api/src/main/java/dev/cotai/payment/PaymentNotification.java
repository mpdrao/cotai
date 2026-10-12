package dev.cotai.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Audit trail of every signed webhook received, including duplicates and rejected ones. */
@Entity
@Table(name = "payment_notification")
public class PaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional: notifications with an unknown reference are stored too (EC10). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "provider_event_id", nullable = false, unique = true, length = 100)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutcome outcome;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected PaymentNotification() {
    }

    public PaymentNotification(Payment payment, String providerEventId, String eventType, String rawPayload,
                               NotificationOutcome outcome, Instant receivedAt) {
        this.payment = payment;
        this.providerEventId = providerEventId;
        this.eventType = eventType;
        this.rawPayload = rawPayload;
        this.outcome = outcome;
        this.receivedAt = receivedAt;
    }

    /** Sets the final outcome (and the payment, when the reference was known). */
    public void resolve(Payment payment, NotificationOutcome outcome) {
        this.payment = payment;
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public NotificationOutcome getOutcome() {
        return outcome;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
