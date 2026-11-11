package dev.cotai.giftlist;

import dev.cotai.common.AppProperties;
import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.eventtype.EventType;
import dev.cotai.eventtype.EventTypeRepository;
import dev.cotai.giftlist.GiftListDtos.CreateGiftListRequest;
import dev.cotai.giftlist.GiftListDtos.GiftListResponse;
import dev.cotai.giftlist.GiftListDtos.ItemInput;
import dev.cotai.giftlist.GiftListDtos.ItemResponse;
import dev.cotai.giftlist.GiftListDtos.UpdateGiftListRequest;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.payment.PaymentRepository;
import dev.cotai.reservation.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GiftListServiceTest {

    @Mock
    private GiftListRepository giftLists;
    @Mock
    private ItemRepository items;
    @Mock
    private EventTypeRepository eventTypes;
    @Mock
    private OrganizerRepository organizers;
    @Mock
    private ReservationRepository reservations;
    @Mock
    private PaymentRepository payments;
    @Mock
    private SlugGenerator slugGenerator;

    private GiftListService service;
    private final Instant now = Instant.parse("2026-11-11T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private Organizer organizer;
    private EventType eventType;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                ZoneOffset.UTC,
                "http://localhost:8080",
                new AppProperties.Jwt("test-secret-at-least-32-chars-long!", 86400),
                "test-manage-secret"
        );
        service = new GiftListService(
                giftLists, items, eventTypes, organizers, reservations, payments, slugGenerator, props, clock
        );

        organizer = new Organizer("Mario", "mario@example.com", "hash", now);
        eventType = new EventType("BARBECUE", "Churrasco", 60);
    }

    @Test
    @DisplayName("create successfully builds and persists GiftList with items")
    void createListSuccess() {
        UUID ownerId = organizer.getId();
        LocalDate eventDate = LocalDate.now(clock).plusDays(5);
        CreateGiftListRequest req = new CreateGiftListRequest(
                "BARBECUE",
                "Churrasco de Sabado",
                eventDate,
                60,
                List.of(new ItemInput("Carvao", "saco", 3000, 1, null))
        );

        when(eventTypes.findById("BARBECUE")).thenReturn(Optional.of(eventType));
        when(organizers.getReferenceById(ownerId)).thenReturn(organizer);
        when(slugGenerator.generate("Churrasco de Sabado")).thenReturn("churrasco-de-sabado");
        when(giftLists.existsBySlug("churrasco-de-sabado")).thenReturn(false);
        when(giftLists.save(any(GiftList.class))).thenAnswer(inv -> inv.getArgument(0));

        GiftListResponse response = service.create(ownerId, req);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Churrasco de Sabado");
        assertThat(response.slug()).isEqualTo("churrasco-de-sabado");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().name()).isEqualTo("Carvao");
        assertThat(response.items().getFirst().unitPriceCents()).isEqualTo(3000);
        assertThat(response.items().getFirst().isDefault()).isFalse();
    }

    @Test
    @DisplayName("addItem adds item to gift list and returns ItemResponse")
    void addItemSuccess() {
        UUID ownerId = organizer.getId();
        GiftList list = new GiftList(organizer, eventType, "Lista", "lista", LocalDate.now(clock).plusDays(5), 60, now);
        UUID listId = list.getId();

        when(giftLists.findWithItemsById(listId)).thenReturn(Optional.of(list));

        ItemInput input = new ItemInput("Picanha", "kg", 8500, 1, null);
        ItemResponse response = service.addItem(ownerId, listId, input);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Picanha");
        assertThat(response.unitPriceCents()).isEqualTo(8500);
        assertThat(response.isDefault()).isFalse();
        verify(giftLists).flush();
    }

    @Test
    @DisplayName("archiveItem throws when item has active reservations")
    void archiveItemWithActiveReservations() {
        UUID ownerId = organizer.getId();
        GiftList list = new GiftList(organizer, eventType, "Lista", "lista", LocalDate.now(clock).plusDays(5), 60, now);
        Item item = list.addItem("Picanha", "kg", 8500);
        UUID listId = list.getId();

        when(giftLists.findWithItemsById(listId)).thenReturn(Optional.of(list));
        when(items.lockById(item.getId())).thenReturn(Optional.of(item));
        when(reservations.hasActiveReservations(eq(item.getId()), any(Instant.class))).thenReturn(true);

        assertThatThrownBy(() -> service.archiveItem(ownerId, listId, item.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.ITEM_HAS_ACTIVE_RESERVATIONS);
    }

    @Test
    @DisplayName("archiveItem successfully marks item as archived when no active reservations")
    void archiveItemSuccess() {
        UUID ownerId = organizer.getId();
        GiftList list = new GiftList(organizer, eventType, "Lista", "lista", LocalDate.now(clock).plusDays(5), 60, now);
        Item item = list.addItem("Picanha", "kg", 8500);
        UUID listId = list.getId();

        when(giftLists.findWithItemsById(listId)).thenReturn(Optional.of(list));
        when(items.lockById(item.getId())).thenReturn(Optional.of(item));
        when(reservations.hasActiveReservations(eq(item.getId()), any(Instant.class))).thenReturn(false);

        service.archiveItem(ownerId, listId, item.getId());

        assertThat(item.isArchived()).isTrue();
    }

    @Test
    @DisplayName("update modifies list details")
    void updateListSuccess() {
        UUID ownerId = organizer.getId();
        GiftList list = new GiftList(organizer, eventType, "Lista Antiga", "lista", LocalDate.now(clock).plusDays(5), 60, now);
        UUID listId = list.getId();

        when(giftLists.findWithItemsById(listId)).thenReturn(Optional.of(list));

        LocalDate newDate = LocalDate.now(clock).plusDays(10);
        UpdateGiftListRequest updateReq = new UpdateGiftListRequest("Lista Nova", newDate, 120);
        GiftListResponse response = service.update(ownerId, listId, updateReq);

        assertThat(response.title()).isEqualTo("Lista Nova");
        assertThat(response.eventDate()).isEqualTo(newDate);
        assertThat(response.reservationTtlMinutes()).isEqualTo(120);
    }
}
