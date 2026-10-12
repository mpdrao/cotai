package dev.cotai.giftlist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GiftListRepository extends JpaRepository<GiftList, UUID> {

    List<GiftList> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = {"items", "eventType"})
    Optional<GiftList> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = {"items", "eventType"})
    Optional<GiftList> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
