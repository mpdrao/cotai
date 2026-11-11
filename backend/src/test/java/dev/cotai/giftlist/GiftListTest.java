package dev.cotai.giftlist;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.eventtype.EventType;
import dev.cotai.organizer.Organizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiftListTest {

    private Organizer organizer;
    private EventType eventType;
    private GiftList giftList;
    private final LocalDate today = LocalDate.of(2026, 11, 11);

    @BeforeEach
    void setUp() {
        organizer = new Organizer("Mario", "mario@example.com", "hash", Instant.now());
        eventType = new EventType("BARBECUE", "Churrasco", 60);

        giftList = new GiftList(
                organizer,
                eventType,
                "Churrasco de Aniversario",
                "churrasco-aniversario",
                today.plusDays(7),
                60,
                Instant.now()
        );
    }

    @Test
    @DisplayName("addItem creates and appends an item with isDefault false")
    void addItemSuccess() {
        Item item = giftList.addItem("Picanha", "kg", 8900);

        assertThat(item).isNotNull();
        assertThat(item.getName()).isEqualTo("Picanha");
        assertThat(item.getUnit()).isEqualTo("kg");
        assertThat(item.getUnitPriceCents()).isEqualTo(8900);
        assertThat(item.isDefault()).isFalse();
        assertThat(item.isArchived()).isFalse();
        assertThat(item.totalCents()).isEqualTo(8900);
        assertThat(giftList.getItems()).contains(item);
    }

    @Test
    @DisplayName("addItem throws when list is CLOSED")
    void addItemWhenClosed() {
        giftList.addItem("Carvao", "saco", 2500);
        giftList.publish(today);
        giftList.close();

        assertThatThrownBy(() -> giftList.addItem("Picanha", "kg", 8900))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("publish transitions status to PUBLISHED")
    void publishSuccess() {
        giftList.addItem("Picanha", "kg", 8900);
        giftList.publish(today);

        assertThat(giftList.getStatus()).isEqualTo(GiftListStatus.PUBLISHED);
        assertThat(giftList.acceptsReservations(today)).isTrue();
    }

    @Test
    @DisplayName("publish throws when list has no items")
    void publishWithoutItems() {
        assertThatThrownBy(() -> giftList.publish(today))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.LIST_HAS_NO_ITEMS);
    }

    @Test
    @DisplayName("publish throws when event date is in the past")
    void publishPastEventDate() {
        giftList.addItem("Picanha", "kg", 8900);
        LocalDate futureToday = today.plusDays(10);

        assertThatThrownBy(() -> giftList.publish(futureToday))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.EVENT_DATE_IN_PAST);
    }

    @Test
    @DisplayName("close transitions status to CLOSED")
    void closeSuccess() {
        giftList.addItem("Picanha", "kg", 8900);
        giftList.publish(today);
        giftList.close();

        assertThat(giftList.getStatus()).isEqualTo(GiftListStatus.CLOSED);
        assertThat(giftList.acceptsReservations(today)).isFalse();
    }

    @Test
    @DisplayName("update modifies title, event date and ttl")
    void updateSuccess() {
        LocalDate newDate = today.plusDays(14);
        giftList.update("Novo Titulo", newDate, 120);

        assertThat(giftList.getTitle()).isEqualTo("Novo Titulo");
        assertThat(giftList.getEventDate()).isEqualTo(newDate);
        assertThat(giftList.getReservationTtlMinutes()).isEqualTo(120);
    }

    @Test
    @DisplayName("isOwnedBy returns true only for matching organizer id")
    void isOwnedByCheck() {
        assertThat(giftList.isOwnedBy(organizer.getId())).isTrue();
        assertThat(giftList.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Item update and archive operations work correctly")
    void itemUpdateAndArchive() {
        Item item = giftList.addItem("Costela", "kg", 4500);
        item.update("Costela Premium", "unidade", 5500);

        assertThat(item.getName()).isEqualTo("Costela Premium");
        assertThat(item.getUnit()).isEqualTo("unidade");
        assertThat(item.getUnitPriceCents()).isEqualTo(5500);
        assertThat(item.totalCents()).isEqualTo(5500);

        item.archive();
        assertThat(item.isArchived()).isTrue();
    }
}
