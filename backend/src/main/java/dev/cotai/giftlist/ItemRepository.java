package dev.cotai.giftlist;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i join fetch i.giftList where i.id = :id")
    Optional<Item> lockById(@Param("id") Long id);

    @Query("""
            select coalesce(sum(r.quantity), 0) from Reservation r
            where r.item.id = :itemId
              and (r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
                   or (r.status = dev.cotai.reservation.ReservationStatus.PENDING and r.expiresAt > :now))
            """)
    int occupiedQuotas(@Param("itemId") Long itemId, @Param("now") Instant now);

    @Query("""
            select r.item.id, sum(r.quantity) from Reservation r
            where r.item.giftList.id = :giftListId
              and (r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
                   or (r.status = dev.cotai.reservation.ReservationStatus.PENDING and r.expiresAt > :now))
            group by r.item.id
            """)
    List<Object[]> occupiedQuotasByItem(@Param("giftListId") UUID giftListId, @Param("now") Instant now);
}
