package dev.cotai.giftlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Photo uploaded by the organizer for an item. Served publicly by its random id (see {@link ItemPhotoController}). */
@Entity
@Table(name = "item_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemPhoto {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false, unique = true)
    private Long itemId;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    ItemPhoto(Long itemId, String contentType, byte[] data, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.itemId = itemId;
        this.contentType = contentType;
        this.data = data;
        this.createdAt = createdAt;
    }

    public String url() {
        return "/api/item-photos/" + id;
    }
}
