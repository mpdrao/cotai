package dev.cotai.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("select r.payment from Reservation r where r.id = :reservationId")
    Optional<Payment> findByReservationId(@Param("reservationId") UUID reservationId);

    @Query("""
            select concat(i.name, ' x', cast(r.quantity as String), ' - ', l.title)
            from Reservation r join r.item i join i.giftList l
            where r.id = :reservationId
            """)
    String chargeDescription(@Param("reservationId") UUID reservationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Payment p set p.status = :to where p.id = :id and p.status = :from")
    int transition(@Param("id") UUID id, @Param("from") PaymentStatus from, @Param("to") PaymentStatus to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Payment p
               set p.status = dev.cotai.payment.PaymentStatus.PENDING,
                   p.providerReference = :ref, p.checkoutUrl = :url, p.qrCodePayload = :qr
             where p.id = :id and p.status = dev.cotai.payment.PaymentStatus.CREATED
            """)
    int markPending(@Param("id") UUID id, @Param("ref") String providerReference,
                    @Param("url") String checkoutUrl, @Param("qr") String qrCodePayload);

    /** PENDING on time, or EXPIRED when the money arrives after the deadline. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Payment p
               set p.status = dev.cotai.payment.PaymentStatus.PAID, p.paidAt = :now
             where p.id = :id
               and p.status in (dev.cotai.payment.PaymentStatus.PENDING, dev.cotai.payment.PaymentStatus.EXPIRED)
            """)
    int markPaid(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Payment p
               set p.status = dev.cotai.payment.PaymentStatus.EXPIRED
             where p.status in (dev.cotai.payment.PaymentStatus.CREATED, dev.cotai.payment.PaymentStatus.PENDING)
               and exists (select 1 from Reservation r
                            where r.payment = p
                              and r.status = dev.cotai.reservation.ReservationStatus.EXPIRED)
            """)
    int expireForExpiredReservations();

    /** Paid, but the reservation was no longer PENDING: it is not reopened. */
    @Query("""
            select coalesce(sum(p.amountCents), 0) from Reservation r join r.payment p
            where p.status = dev.cotai.payment.PaymentStatus.PAID
              and r.status <> dev.cotai.reservation.ReservationStatus.CONFIRMED
              and r.item.giftList.id = :giftListId
            """)
    long sumLatePayments(@Param("giftListId") UUID giftListId);
}
