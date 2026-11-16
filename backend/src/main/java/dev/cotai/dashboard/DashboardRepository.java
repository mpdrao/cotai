package dev.cotai.dashboard;

import dev.cotai.dashboard.DashboardDtos.Contribution;
import dev.cotai.dashboard.DashboardDtos.ContributionTotals;
import dev.cotai.dashboard.DashboardDtos.ItemAggregate;
import dev.cotai.reservation.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-only queries for the organizer dashboard. They return projections, never entities. */
public interface DashboardRepository extends JpaRepository<Reservation, UUID> {

    @Query("""
            select new dev.cotai.dashboard.DashboardDtos$ItemAggregate(
                   i.id,
                   coalesce(sum(case when r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
                                     then r.quantity else 0 end), 0),
                   coalesce(sum(case when r.status = dev.cotai.reservation.ReservationStatus.PENDING
                                          and p.expiresAt > :now then r.quantity else 0 end), 0),
                   coalesce(sum(case when r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
                                     then r.amountCents else 0 end), 0),
                   coalesce(sum(case when r.status = dev.cotai.reservation.ReservationStatus.PENDING
                                          and p.expiresAt > :now then r.amountCents else 0 end), 0))
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId
            group by i.id
            """)
    List<ItemAggregate> aggregateByItem(@Param("giftListId") UUID giftListId, @Param("now") Instant now);

    @Query("""
            select new dev.cotai.dashboard.DashboardDtos$ContributionTotals(
                   count(r), count(distinct r.guestEmail), max(p.paidAt))
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId
              and r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
              and p.status = dev.cotai.payment.PaymentStatus.PAID
            """)
    ContributionTotals contributionTotals(@Param("giftListId") UUID giftListId);

    @Query(value = """
            select new dev.cotai.dashboard.DashboardDtos$Contribution(
                   r.id, r.guestName, i.id, i.name, r.quantity, p.amountCents, p.paidAt, r.message)
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId
              and r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
              and p.status = dev.cotai.payment.PaymentStatus.PAID
            order by p.paidAt desc, r.id
            """,
            countQuery = """
            select count(r)
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId
              and r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
              and p.status = dev.cotai.payment.PaymentStatus.PAID
            """)
    Page<Contribution> findContributions(@Param("giftListId") UUID giftListId, Pageable pageable);

    @Query(value = """
            select new dev.cotai.dashboard.DashboardDtos$Contribution(
                   r.id, r.guestName, i.id, i.name, r.quantity, p.amountCents, p.paidAt, r.message)
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId and i.id = :itemId
              and r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
              and p.status = dev.cotai.payment.PaymentStatus.PAID
            order by p.paidAt desc, r.id
            """,
            countQuery = """
            select count(r)
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId and i.id = :itemId
              and r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
              and p.status = dev.cotai.payment.PaymentStatus.PAID
            """)
    Page<Contribution> findContributionsByItem(@Param("giftListId") UUID giftListId, @Param("itemId") Long itemId,
                                               Pageable pageable);

    @Query("""
            select new dev.cotai.dashboard.DashboardDtos$Contribution(
                   r.id, r.guestName, i.id, i.name, r.quantity, p.amountCents, p.paidAt, r.message)
            from Reservation r join r.item i join r.payment p
            where i.giftList.id = :giftListId
              and r.status = dev.cotai.reservation.ReservationStatus.CONFIRMED
              and p.status = dev.cotai.payment.PaymentStatus.PAID
            order by p.paidAt, r.id
            """)
    List<Contribution> findAllContributions(@Param("giftListId") UUID giftListId);
}
