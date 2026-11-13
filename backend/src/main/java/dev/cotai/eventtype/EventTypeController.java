package dev.cotai.eventtype;

import dev.cotai.common.BusinessException;
import dev.cotai.giftlist.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/event-types")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventTypeController {

    private final EventTypeRepository eventTypes;

    @GetMapping
    public List<EventTypeResponse> list() {
        return eventTypes.findAllByOrderByNameAsc().stream()
                .map(EventTypeResponse::of)
                .toList();
    }

    @GetMapping("/{eventTypeCode}/template-items")
    public List<TemplateItemResponse> templateItems(@PathVariable String eventTypeCode) {
        Event event = eventTypes.findById(eventTypeCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> BusinessException.notFound("Event type"));

        List<Item> suggestions = event.suggestedItems();
        return IntStream.range(0, suggestions.size())
                .mapToObj(i -> TemplateItemResponse.of(i + 1, suggestions.get(i)))
                .toList();
    }

    public record EventTypeResponse(String code, String name, int defaultReservationTtlMinutes) {

        static EventTypeResponse of(Event event) {
            return new EventTypeResponse(event.getCode(), event.getName(), event.getDefaultReservationTtlMinutes());
        }
    }

    public record TemplateItemResponse(int position, String name, String unit, long unitPriceCents,
                                       int suggestedQuotas, String imageUrl) {

        static TemplateItemResponse of(int position, Item item) {
            // Item has no quota count in the model, so every suggestion starts with one quota.
            return new TemplateItemResponse(position, item.getName(), item.getUnit(),
                    item.getUnitPriceCents(), 1, item.getImageUrl());
        }
    }
}
