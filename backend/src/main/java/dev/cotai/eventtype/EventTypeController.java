package dev.cotai.eventtype;

import dev.cotai.eventtype.dto.EventTypeResponse;
import dev.cotai.eventtype.dto.TemplateItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/event-types")
@RequiredArgsConstructor
public class EventTypeController {

    private final EventTypeService eventTypeService;

    @GetMapping
    public List<EventTypeResponse> list() {
        return eventTypeService.list();
    }

    @GetMapping("/{eventTypeCode}/template-items")
    public List<TemplateItemResponse> templateItems(@PathVariable String eventTypeCode) {
        return eventTypeService.templateItems(eventTypeCode);
    }
}
