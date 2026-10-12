package dev.cotai.giftlist;

import dev.cotai.giftlist.GiftListDtos.PublicGiftList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Guest view of a published list, opened from the shared link. */
@RestController
@RequestMapping("/api/public/gift-lists")
public class PublicGiftListController {

    private final GiftListService service;

    public PublicGiftListController(GiftListService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public PublicGiftList get(@PathVariable String slug) {
        return service.publicView(slug);
    }
}
