package dev.cotai.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {

    Optional<PaymentNotification> findByProviderEventId(String providerEventId);

    /**
     * {@code INSERT ... ON CONFLICT DO NOTHING}. Returns 0 when the event id was already stored
     * (duplicate delivery). Concurrent duplicates block on the unique index until the first commits.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into payment_notification (provider_event_id, event_type, raw_payload, outcome, received_at)
            values (:eventId, :eventType, :rawPayload, 'APPLIED', :receivedAt)
            on conflict (provider_event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String providerEventId, @Param("eventType") String eventType,
                       @Param("rawPayload") String rawPayload, @Param("receivedAt") Instant receivedAt);
}
