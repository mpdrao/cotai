package dev.cotai.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, String> {

    /** Catalog Events (US-07: event types) are the ones not based on another Event. */
    List<Event> findAllByEventIsNullOrderByNameAsc();

    default Optional<Event> findCatalogEvent(String code) {
        return findById(code).filter(e -> e.getEvent() == null);
    }

    /** The Event of an organizer's list, with the catalog Event it was based on. */
    @Query("select e from Event e left join fetch e.event where e.giftList.id = :giftListId")
    Optional<Event> findByGiftListId(@Param("giftListId") UUID giftListId);
}
