package dev.cotai.reservation;

import dev.cotai.security.AppProperties;
import dev.cotai.common.BusinessException;
import dev.cotai.common.Crypto;
import dev.cotai.common.ErrorCode;
import dev.cotai.event.Event;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.Item;
import dev.cotai.giftlist.ItemRepository;
import dev.cotai.organizer.Organizer;
import dev.cotai.payment.Payment;
import dev.cotai.payment.PaymentGateway;
import dev.cotai.payment.PaymentProperties;
import dev.cotai.payment.PaymentRepository;
import dev.cotai.payment.PaymentStatus;
import dev.cotai.reservation.ReservationDtos.ReserveOutcome;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ItemRepository items;
    @Mock
    private ReservationRepository reservations;
    @Mock
    private PaymentRepository payments;
    @Mock
    private PaymentGateway gateway;

    private ReservationService service;
    private final Instant now = Instant.parse("2026-11-15T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AppProperties props = new AppProperties(ZoneOffset.UTC, "http://localhost:8080",
            new AppProperties.Jwt("test-secret-at-least-32-chars-long!", 3600), "test-manage-secret");
    private GiftList list;
    private Item item;

    @BeforeEach
    void setUp() {
        service = new ReservationService(items, reservations, payments, gateway, props,
                new PaymentProperties("webhook-secret", 3000, 3), clock);

        Organizer organizer = new Organizer("Ana", "ana@example.com", "hash", now);
        list = new GiftList(organizer, new Event("BARBECUE", "Churrasco", 30), "Churrasco", "churrasco",
                LocalDate.of(2026, 12, 1), 30, now);
        item = list.addItem("Carvao", "saco", 4500);
        item.setId(10L);
        list.publish(LocalDate.of(2026, 11, 15));
    }

    private ReserveRequest request(int quantity) {
        return new ReserveRequest(" Bia ", "Bia@Example.com ", quantity);
    }

    @Test
    @DisplayName("reserve creates a PENDING reservation linked to a CREATED payment")
    void reserveLinksPayment() {
        when(reservations.findByManageTokenHash(anyString())).thenReturn(Optional.empty());
        when(items.lockById(10L)).thenReturn(Optional.of(item));
        when(items.occupiedQuotas(10L, now)).thenReturn(0);
        when(gateway.providerName()).thenReturn("mock");
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        ReserveOutcome outcome = service.reserve(10L, request(1), UUID.randomUUID());

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).saveAndFlush(saved.capture());
        Reservation r = saved.getValue();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(r.getGuestName()).isEqualTo("Bia");
        assertThat(r.getGuestEmail()).isEqualTo("bia@example.com");
        assertThat(r.getPayment()).isNotNull();
        assertThat(r.getPayment().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(r.getPayment().getAmountCents()).isEqualTo(4500);
        assertThat(r.getPayment().getExpiresAt()).isEqualTo(now.plusSeconds(30 * 60));
        assertThat(outcome.replay()).isFalse();
        assertThat(outcome.body().payment().paymentId()).isEqualTo(r.getPayment().getId());
        assertThat(outcome.body().manageToken()).isNotBlank();
    }

    @Test
    @DisplayName("reserve refuses more quotas than the free item has")
    void reserveRefusesMoreThanAvailable() {
        when(reservations.findByManageTokenHash(anyString())).thenReturn(Optional.empty());
        when(items.lockById(10L)).thenReturn(Optional.of(item));
        when(items.occupiedQuotas(10L, now)).thenReturn(0);

        assertThatThrownBy(() -> service.reserve(10L, request(2), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.QUOTA_UNAVAILABLE);
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reserve refuses an item that is already reserved")
    void reserveRefusesOccupiedItem() {
        when(reservations.findByManageTokenHash(anyString())).thenReturn(Optional.empty());
        when(items.lockById(10L)).thenReturn(Optional.of(item));
        when(items.occupiedQuotas(10L, now)).thenReturn(1);

        assertThatThrownBy(() -> service.reserve(10L, request(1), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.QUOTA_UNAVAILABLE);
    }

    @Test
    @DisplayName("reserve refuses a list that is not accepting reservations")
    void reserveRefusesClosedList() {
        list.close();
        when(reservations.findByManageTokenHash(anyString())).thenReturn(Optional.empty());
        when(items.lockById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.reserve(10L, request(1), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.LIST_NOT_ACCEPTING_RESERVATIONS);
    }

    @Test
    @DisplayName("reserve refuses a guest with too many pending reservations")
    void reserveRefusesTooManyPending() {
        when(reservations.findByManageTokenHash(anyString())).thenReturn(Optional.empty());
        when(items.lockById(10L)).thenReturn(Optional.of(item));
        when(reservations.countActivePendingByEmail(list.getId(), "bia@example.com", now)).thenReturn(3);

        assertThatThrownBy(() -> service.reserve(10L, request(1), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.TOO_MANY_PENDING_RESERVATIONS);
    }

    @Test
    @DisplayName("the same Idempotency-Key replays the existing reservation instead of creating another")
    void reserveReplaysSameKey() {
        Reservation existing = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1, "hash", now);
        existing.setPayment(new Payment("mock", 4500, now.plusSeconds(1800)));
        when(reservations.findByManageTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(reservations.findWithDetailsById(existing.getId())).thenReturn(Optional.of(existing));

        ReserveOutcome outcome = service.reserve(10L, request(1), UUID.randomUUID());

        assertThat(outcome.replay()).isTrue();
        assertThat(outcome.body().reservationId()).isEqualTo(existing.getId());
        verify(reservations, never()).saveAndFlush(any());
        verify(payments, never()).save(any());
    }

    @Test
    @DisplayName("get refuses a wrong manage token")
    void getRefusesWrongToken() {
        Reservation r = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1,
                Crypto.sha256Hex("right-token"), now);
        r.setPayment(new Payment("mock", 4500, now.plusSeconds(1800)));
        when(reservations.findWithDetailsById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.get(r.getId(), "wrong-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(service.get(r.getId(), "right-token").payment().paymentId()).isEqualTo(r.getPayment().getId());
    }

    @Test
    @DisplayName("cancel only works while the reservation is PENDING")
    void cancelOnlyPending() {
        Reservation r = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1,
                Crypto.sha256Hex("token"), now);
        r.setStatus(ReservationStatus.CONFIRMED);
        when(reservations.findWithDetailsById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(r.getId(), "token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        verify(reservations, never()).markCancelled(eq(r.getId()), any());
    }
}
