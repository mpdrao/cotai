package dev.cotai.event;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.giftlist.GiftListDtos.ItemResponse;
import dev.cotai.giftlist.Item;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository events;

    public EventController(EventRepository events) {
        this.events = events;
    }

    @GetMapping
    public List<EventResponse> list() {
        return events.findAllByOrderByNameAsc().stream()
                .map(EventResponse::of)
                .toList();
    }

    @GetMapping("/{eventCode}/items")
    @Transactional(readOnly = true)
    public List<ItemResponse> items(@PathVariable String eventCode) {
        Event event = events.findById(eventCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Event not found: " + eventCode));

        return event.getItems().stream()
                .filter(Item::isDefault)
                .filter(item -> !item.isArchived())
                .map(ItemResponse::of)
                .toList();
    }

    public record EventResponse(String code, String name, int defaultReservationTtlMinutes, String eventCode, UUID giftListId) {
        static EventResponse of(Event event) {
            String parentEventCode = event.getEvent() != null ? event.getEvent().getCode() : null;
            UUID giftListId = event.getGiftList() != null ? event.getGiftList().getId() : null;
            return new EventResponse(event.getCode(), event.getName(), event.getDefaultReservationTtlMinutes(), parentEventCode, giftListId);
        }
    }
}
