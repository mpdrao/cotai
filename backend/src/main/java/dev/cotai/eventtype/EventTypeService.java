package dev.cotai.eventtype;

import dev.cotai.common.BusinessException;
import dev.cotai.eventtype.dto.EventTypeResponse;
import dev.cotai.eventtype.dto.TemplateItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventTypeService {

    private final EventTypeRepository eventTypes;

    public List<EventTypeResponse> list() {
        return eventTypes.findAllByOrderByNameAsc().stream()
                .map(e -> new EventTypeResponse(e.getCode(), e.getName(), e.getDefaultReservationTtlMinutes()))
                .toList();
    }

    public List<TemplateItemResponse> templateItems(String eventTypeCode) {
        EventType eventType = eventTypes.findById(eventTypeCode)
                .orElseThrow(() -> BusinessException.notFound("Event type"));
        return eventType.getTemplateItems().stream()
                .map(t -> new TemplateItemResponse(t.getPosition(), t.getName(), t.getUnit(),
                        t.getUnitPriceCents(), t.getSuggestedQuotas(), t.getImageUrl()))
                .toList();
    }
}
