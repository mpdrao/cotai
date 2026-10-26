package dev.cotai.giftlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ItemPhotoRepository extends JpaRepository<ItemPhoto, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("delete from ItemPhoto p where p.itemId = :itemId")
    void deleteByItemId(Long itemId);
}
