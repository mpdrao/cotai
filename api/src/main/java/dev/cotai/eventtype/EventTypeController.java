package dev.cotai.eventtype;

import dev.cotai.common.BusinessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/event-types")
@Transactional(readOnly = true)
public class EventTypeController {

    public record EventTypeResponse(String code, String name, int defaultReservationTtlMinutes) {
    }

    public record TemplateItemResponse(int position, String name, String unit, long unitPriceCents,
                                       int suggestedQuotas) {
    }

    private final EventTypeRepository eventTypes;

    public EventTypeController(EventTypeRepository eventTypes) {
        this.eventTypes = eventTypes;
    }

    @GetMapping
    public List<EventTypeResponse> list() {
        return eventTypes.findAllByOrderByNameAsc().stream()
                .map(e -> new EventTypeResponse(e.getCode(), e.getName(), e.getDefaultReservationTtlMinutes()))
                .toList();
    }

    @GetMapping("/{eventTypeCode}/template-items")
    public List<TemplateItemResponse> templateItems(@PathVariable String eventTypeCode) {
        EventType eventType = eventTypes.findById(eventTypeCode)
                .orElseThrow(() -> BusinessException.notFound("Event type"));
        return eventType.getTemplateItems().stream()
                .map(t -> new TemplateItemResponse(t.getPosition(), t.getName(), t.getUnit(),
                        t.getUnitPriceCents(), t.getSuggestedQuotas()))
                .toList();
    }
}
