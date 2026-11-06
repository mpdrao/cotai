package dev.cotai.giftlist;

import dev.cotai.giftlist.dto.PublicGiftList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/gift-lists")
@RequiredArgsConstructor
public class PublicGiftListController {

    private final GiftListService service;

    @GetMapping("/{slug}")
    public PublicGiftList get(@PathVariable String slug) {
        return service.publicView(slug);
    }
}
