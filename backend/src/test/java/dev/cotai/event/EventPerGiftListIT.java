package dev.cotai.event;

import dev.cotai.TestcontainersConfiguration;
import dev.cotai.devsupport.MutableClock;
import dev.cotai.giftlist.GiftListDtos.CreateGiftListRequest;
import dev.cotai.giftlist.GiftListDtos.GiftListResponse;
import dev.cotai.giftlist.GiftListDtos.ItemInput;
import dev.cotai.giftlist.GiftListDtos.UpdateGiftListRequest;
import dev.cotai.giftlist.GiftListService;
import dev.cotai.giftlist.Item;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Event - giftList 1, Item - giftList 1 and Event - event 0..1 against a real PostgreSQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class EventPerGiftListIT {

    @Autowired
    private EventRepository events;
    @Autowired
    private GiftListService giftLists;
    @Autowired
    private OrganizerRepository organizers;
    @Autowired
    private MutableClock clock;

    @Test
    @Transactional
    @DisplayName("each catalog Event belongs to a template list that holds its suggested items")
    void catalogEventsHaveTemplateLists() {
        List<Event> catalog = events.findAllByEventIsNullOrderByNameAsc();

        assertThat(catalog).extracting(Event::getCode)
                .contains("BARBECUE", "BABY_SHOWER", "WEDDING", "BIRTHDAY");
        for (Event type : catalog) {
            assertThat(type.getEvent()).isNull();
            assertThat(type.getGiftList()).isNotNull();
            assertThat(type.getGiftList().getOwner().getEmail()).isEqualTo("catalogo@cotai.local");
            assertThat(type.getItem()).allSatisfy(item -> {
                assertThat(item.isDefault()).isTrue();
                assertThat(item.getGiftList().getId()).isEqualTo(type.getGiftList().getId());
            });
        }
        Event barbecue = events.findCatalogEvent("BARBECUE").orElseThrow();
        assertThat(barbecue.getItem()).hasSize(10).extracting(Item::getName).contains("Picanha");
    }

    @Test
    @Transactional
    @DisplayName("US-07: creating a list gives it its own Event, based on the catalog Event picked")
    void listGetsItsOwnEvent() {
        Organizer owner = organizers.save(new Organizer("Ana", "ana-" + UUID.randomUUID() + "@example.com",
                "hash", clock.instant()));
        LocalDate eventDate = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(20);

        GiftListResponse created = giftLists.create(owner.getId(), new CreateGiftListRequest("barbecue",
                "Churrasco da firma", eventDate, null, List.of(new ItemInput("Picanha", "kg", 9000, null, null))));

        assertThat(created.items()).singleElement().satisfies(item -> assertThat(item.id()).isNotNull());

        Event own = events.findByGiftListId(created.id()).orElseThrow();
        assertThat(own.getCode()).isEqualTo(created.slug());
        assertThat(own.getEvent().getCode()).isEqualTo("BARBECUE");
        assertThat(own.getName()).isEqualTo("Churrasco");
        assertThat(created.eventCode()).isEqualTo("BARBECUE");

        GiftListResponse changed = giftLists.update(owner.getId(), created.id(),
                new UpdateGiftListRequest(null, null, null, "birthday"));

        Event afterChange = events.findByGiftListId(created.id()).orElseThrow();
        assertThat(changed.eventCode()).isEqualTo("BIRTHDAY");
        assertThat(afterChange.getCode()).isEqualTo(created.slug());
        assertThat(afterChange.getEvent().getCode()).isEqualTo("BIRTHDAY");
        assertThat(afterChange.getName()).isEqualTo("Aniversário");
        assertThat(changed.items()).extracting(i -> i.name()).containsExactly("Picanha");
    }
}
