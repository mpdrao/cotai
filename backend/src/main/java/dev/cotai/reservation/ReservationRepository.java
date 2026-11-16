package dev.cotai.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByManageTokenHash(String manageTokenHash);

    @Query("""
            select r from Reservation r join fetch r.payment join fetch r.item i join fetch i.giftList
            where r.id = :id
            """)
    Optional<Reservation> findWithDetailsById(@Param("id") UUID id);

    @Query("select r from Reservation r join fetch r.payment p where p.providerReference = :ref")
    Optional<Reservation> findByPaymentProviderReference(@Param("ref") String providerReference);

    @Query("""
            select count(r) from Reservation r join r.payment p
            where r.item.giftList.id = :giftListId
              and r.guestEmail = :email
              and r.status = dev.cotai.reservation.ReservationStatus.PENDING
              and p.expiresAt > :now
            """)
    int countActivePendingByEmail(@Param("giftListId") UUID giftListId, @Param("email") String email,
                                  @Param("now") Instant now);

    @Query("""
            select count(r) > 0 from Reservation r join r.payment p
            where r.item.id = :itemId
              and (r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
                   or (r.status = dev.cotai.reservation.ReservationStatus.PENDING and p.expiresAt > :now))
            """)
    boolean hasActiveReservations(@Param("itemId") Long itemId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED, r.closedAt = :now
             where r.id = :id
               and r.status = dev.cotai.reservation.ReservationStatus.PENDING
               and r.payment.id in (select p.id from Payment p where p.expiresAt > :now)
            """)
    int markConfirmed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = dev.cotai.reservation.ReservationStatus.CANCELLED, r.closedAt = :now
             where r.id = :id
               and r.status = dev.cotai.reservation.ReservationStatus.PENDING
            """)
    int markCancelled(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = dev.cotai.reservation.ReservationStatus.EXPIRED, r.closedAt = :now
             where r.status = dev.cotai.reservation.ReservationStatus.PENDING
               and r.payment.id in (select p.id from Payment p where p.expiresAt <= :now)
            """)
    int markExpiredOverdue(@Param("now") Instant now);
}
