package dev.cotai.eventtype;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.eventtype.EventTypeController.EventTypeResponse;
import dev.cotai.eventtype.EventTypeController.TemplateItemResponse;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventTypeControllerTest {

    @Mock
    private EventTypeRepository eventTypes;

    private EventTypeController controller;
    private Event birthday;

    @BeforeEach
    void setUp() {
        controller = new EventTypeController(eventTypes);

        birthday = new Event("BIRTHDAY", "Aniversário", 30);
        Item cake = new Item(birthday, "Bolo", "unidade", 15000, null);
        Item oldSuggestion = new Item(birthday, "Língua de sogra", "pacote", 1000, null);
        oldSuggestion.archive();
        Item balloons = new Item(birthday, "Balões", "pacote", 2500, "/suggestions/birthday-8.jpg");
        Item notASuggestion = new Item((GiftList) null, "Item de uma lista", "unidade", 500, null);

        // The mapped collection is filled by JPA in production.
        ReflectionTestUtils.setField(birthday, "items",
                new ArrayList<>(List.of(cake, oldSuggestion, balloons, notASuggestion)));
    }

    @Test
    @DisplayName("list returns the catalog with the default payment window")
    void listReturnsCatalog() {
        Event wedding = new Event("WEDDING", "Casamento", 45);
        when(eventTypes.findAllByOrderByNameAsc()).thenReturn(List.of(birthday, wedding));

        assertThat(controller.list()).containsExactly(
                new EventTypeResponse("BIRTHDAY", "Aniversário", 30),
                new EventTypeResponse("WEDDING", "Casamento", 45));
    }

    @Test
    @DisplayName("template items are the active suggestions, numbered from 1")
    void templateItemsAreActiveSuggestions() {
        when(eventTypes.findById("BIRTHDAY")).thenReturn(Optional.of(birthday));

        List<TemplateItemResponse> items = controller.templateItems("BIRTHDAY");

        assertThat(items).containsExactly(
                new TemplateItemResponse(1, "Bolo", "unidade", 15000, 1, null),
                new TemplateItemResponse(2, "Balões", "pacote", 2500, 1, "/suggestions/birthday-8.jpg"));
    }

    @Test
    @DisplayName("event type code in the URL is not case sensitive")
    void codeIsNotCaseSensitive() {
        when(eventTypes.findById("BIRTHDAY")).thenReturn(Optional.of(birthday));

        assertThat(controller.templateItems("birthday")).hasSize(2);
    }

    @Test
    @DisplayName("unknown event type is 404")
    void unknownEventType() {
        when(eventTypes.findById("GRADUATION")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.templateItems("GRADUATION"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
