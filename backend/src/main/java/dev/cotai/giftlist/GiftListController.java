package dev.cotai.giftlist;

import dev.cotai.giftlist.GiftListDtos.CreateGiftListRequest;
import dev.cotai.giftlist.GiftListDtos.GiftListResponse;
import dev.cotai.giftlist.GiftListDtos.GiftListSummary;
import dev.cotai.giftlist.GiftListDtos.GiftListSummaryReport;
import dev.cotai.giftlist.GiftListDtos.ItemInput;
import dev.cotai.giftlist.GiftListDtos.ItemResponse;
import dev.cotai.giftlist.GiftListDtos.UpdateGiftListRequest;
import dev.cotai.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Organizer area. Requires a bearer token ({@code /api/gift-lists/**}). */
@RestController
@RequestMapping("/api/gift-lists")
@RequiredArgsConstructor
public class GiftListController {

    private final GiftListService service;

    @PostMapping
    public ResponseEntity<GiftListResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CreateGiftListRequest body) {
        GiftListResponse created = service.create(JwtService.organizerId(jwt), body);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/api/gift-lists/" + created.id()))
                .body(created);
    }

    @GetMapping
    public List<GiftListSummary> listMine(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(JwtService.organizerId(jwt));
    }

    @GetMapping("/{giftListId}")
    public GiftListResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId) {
        return service.get(JwtService.organizerId(jwt), giftListId);
    }

    @PatchMapping("/{giftListId}")
    public GiftListResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                   @Valid @RequestBody UpdateGiftListRequest body) {
        return service.update(JwtService.organizerId(jwt), giftListId, body);
    }

    @PostMapping("/{giftListId}/publish")
    public GiftListResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId) {
        return service.publish(JwtService.organizerId(jwt), giftListId);
    }

    @PostMapping("/{giftListId}/close")
    public GiftListResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId) {
        return service.close(JwtService.organizerId(jwt), giftListId);
    }

    @PostMapping("/{giftListId}/items")
    public ResponseEntity<ItemResponse> addItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                                @Valid @RequestBody ItemInput body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addItem(JwtService.organizerId(jwt), giftListId, body));
    }

    @PutMapping("/{giftListId}/items/{itemId}")
    public ItemResponse updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                   @PathVariable Long itemId, @Valid @RequestBody ItemInput body) {
        return service.updateItem(JwtService.organizerId(jwt), giftListId, itemId, body);
    }

    @DeleteMapping("/{giftListId}/items/{itemId}")
    public ResponseEntity<Void> archiveItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                            @PathVariable Long itemId) {
        service.archiveItem(JwtService.organizerId(jwt), giftListId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/{giftListId}/items/{itemId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse uploadPhoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                    @PathVariable Long itemId, @RequestPart("file") MultipartFile file)
            throws IOException {
        return service.uploadPhoto(JwtService.organizerId(jwt), giftListId, itemId, file.getBytes());
    }

    @DeleteMapping("/{giftListId}/items/{itemId}/photo")
    public ItemResponse removePhoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                    @PathVariable Long itemId) {
        return service.removePhoto(JwtService.organizerId(jwt), giftListId, itemId);
    }

    @GetMapping("/{giftListId}/summary")
    public GiftListSummaryReport summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId) {
        return service.summary(JwtService.organizerId(jwt), giftListId);
    }
}
