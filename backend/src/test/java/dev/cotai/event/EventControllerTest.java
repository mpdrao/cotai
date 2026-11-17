package dev.cotai.event;

import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListDtos;
import dev.cotai.giftlist.Item;
import dev.cotai.organizer.Organizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventRepository events;

    private EventController controller;

    private static GiftList catalogList() {
        Organizer system = new Organizer("Cota Ae! (catálogo)", "catalogo@cotai.local", "!", Instant.now());
        return new GiftList(system, "Catálogo", "catalogo", LocalDate.of(2099, 12, 31), 30, Instant.now());
    }

    @BeforeEach
    void setUp() {
        controller = new EventController(events);
    }

    @Test
    void listReturnsAllEvents() {
        Event wedding = new Event("WEDDING", "Casamento", 30, null, catalogList());
        Event barbecue = new Event("BARBECUE", "Churrasco", 30, null, catalogList());
        when(events.findAllByEventIsNullOrderByNameAsc()).thenReturn(List.of(wedding, barbecue));

        List<EventController.EventResponse> response = controller.list();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).code()).isEqualTo("WEDDING");
        assertThat(response.get(1).code()).isEqualTo("BARBECUE");
    }

    @Test
    void itemsReturnsItemsForEvent() {
        GiftList catalog = catalogList();
        Event event = new Event("BARBECUE", "Churrasco", 30, null, catalog);
        Item item = new Item(catalog, "Picanha", "kg", 9000, null);
        item.setDefault(true);
        ReflectionTestUtils.setField(event, "item", new ArrayList<>(List.of(item)));
        when(events.findCatalogEvent("BARBECUE")).thenReturn(Optional.of(event));

        List<GiftListDtos.ItemResponse> response = controller.items("barbecue");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("Picanha");
    }
}
