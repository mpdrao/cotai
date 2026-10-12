package dev.cotai.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Every status change is a conditional UPDATE ({@code WHERE status = :from}); 0 rows = lost the race. */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("select p from Payment p join fetch p.reservation r join fetch r.item i join fetch i.giftList where r.id = :reservationId")
    Optional<Payment> findByReservationIdWithDetails(@Param("reservationId") UUID reservationId);

    @Query("select p from Payment p join fetch p.reservation where p.providerReference = :ref")
    Optional<Payment> findByProviderReference(@Param("ref") String providerReference);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Payment p set p.status = :to where p.id = :id and p.status = :from")
    int transition(@Param("id") UUID id, @Param("from") PaymentStatus from, @Param("to") PaymentStatus to);

    /** CREATED -> PENDING once the gateway returned the charge. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Payment p
               set p.status = dev.cotai.payment.PaymentStatus.PENDING,
                   p.providerReference = :ref, p.checkoutUrl = :url, p.qrCodePayload = :qr
             where p.id = :id and p.status = dev.cotai.payment.PaymentStatus.CREATED
            """)
    int markPending(@Param("id") UUID id, @Param("ref") String providerReference,
                    @Param("url") String checkoutUrl, @Param("qr") String qrCodePayload);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Payment p
               set p.status = dev.cotai.payment.PaymentStatus.PAID, p.paidAt = :now
             where p.id = :id and p.status = dev.cotai.payment.PaymentStatus.PENDING
            """)
    int markPaid(@Param("id") UUID id, @Param("now") Instant now);

    /** Payments whose reservation already expired become EXPIRED (called by the expiry job). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Payment p
               set p.status = dev.cotai.payment.PaymentStatus.EXPIRED
             where p.status in (dev.cotai.payment.PaymentStatus.CREATED, dev.cotai.payment.PaymentStatus.PENDING)
               and exists (select 1 from Reservation r
                            where r.id = p.reservation.id
                              and r.status = dev.cotai.reservation.ReservationStatus.EXPIRED)
            """)
    int expireForExpiredReservations();

    @Query("""
            select coalesce(sum(p.amountCents), 0) from Payment p
            where p.status = dev.cotai.payment.PaymentStatus.REFUND_REQUIRED
              and p.reservation.item.giftList.id = :giftListId
            """)
    long sumRefundRequired(@Param("giftListId") UUID giftListId);
}
