package dev.cotai.giftlist;

import dev.cotai.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/** Public: guests see item photos on the shared list. The id is random and changes on every upload. */
@RestController
@RequiredArgsConstructor
public class ItemPhotoController {

    private final ItemPhotoRepository photos;

    @GetMapping("/api/item-photos/{photoId}")
    public ResponseEntity<byte[]> get(@PathVariable UUID photoId) {
        ItemPhoto photo = photos.findById(photoId).orElseThrow(() -> BusinessException.notFound("Photo"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(photo.getData());
    }
}
