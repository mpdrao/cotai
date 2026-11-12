package dev.cotai.eventtype;

import dev.cotai.common.BusinessException;
import dev.cotai.giftlist.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/event-types")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventTypeController {

    public record EventTypeResponse(String code, String name, int defaultReservationTtlMinutes) {
    }

    public record TemplateItemResponse(int position, String name, String unit, long unitPriceCents,
                                       int suggestedQuotas, String imageUrl) {
    }

    private final EventTypeRepository eventTypes;

    @GetMapping
    public List<EventTypeResponse> list() {
        return eventTypes.findAllByOrderByNameAsc().stream()
                .map(e -> new EventTypeResponse(e.getCode(), e.getName(), e.getDefaultReservationTtlMinutes()))
                .toList();
    }

    @GetMapping("/{eventTypeCode}/template-items")
    public List<TemplateItemResponse> templateItems(@PathVariable String eventTypeCode) {
        Event event = eventTypes.findById(eventTypeCode)
                .orElseThrow(() -> BusinessException.notFound("Event type"));
        List<Item> defaultItems = event.getItems().stream()
                .filter(Item::isDefault)
                .toList();
        List<TemplateItemResponse> responses = new ArrayList<>(defaultItems.size());
        for (int i = 0; i < defaultItems.size(); i++) {
            Item item = defaultItems.get(i);
            responses.add(new TemplateItemResponse(i + 1, item.getName(), item.getUnit(),
                    item.getUnitPriceCents(), 1, item.getImageUrl()));
        }
        return responses;
    }
}
