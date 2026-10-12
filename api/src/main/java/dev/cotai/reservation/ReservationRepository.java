package dev.cotai.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every transition is a conditional UPDATE ({@code WHERE status = 'PENDING'}). A result of 0 rows
 * means another transaction already changed the state.
 */
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdempotencyKey(UUID idempotencyKey);

    @Query("""
            select count(r) from Reservation r
            where r.item.giftList.id = :giftListId
              and r.guestEmail = :email
              and r.status = dev.cotai.reservation.ReservationStatus.PENDING
              and r.expiresAt > :now
            """)
    int countActivePendingByEmail(@Param("giftListId") UUID giftListId, @Param("email") String email,
                                  @Param("now") Instant now);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.item.id = :itemId
              and (r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
                   or (r.status = dev.cotai.reservation.ReservationStatus.PENDING and r.expiresAt > :now))
            """)
    boolean hasActiveReservations(@Param("itemId") Long itemId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED, r.closedAt = :now
             where r.id = :id
               and r.status = dev.cotai.reservation.ReservationStatus.PENDING
               and r.expiresAt > :now
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

    /** Idempotent: safe to run on N instances at the same time (EC16). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = dev.cotai.reservation.ReservationStatus.EXPIRED, r.closedAt = :now
             where r.status = dev.cotai.reservation.ReservationStatus.PENDING
               and r.expiresAt <= :now
            """)
    int markExpiredOverdue(@Param("now") Instant now);

    @Query("""
            select r from Reservation r join fetch r.item i
            where i.giftList.id = :giftListId
            order by r.createdAt
            """)
    List<Reservation> findAllByGiftListId(@Param("giftListId") UUID giftListId);
}
