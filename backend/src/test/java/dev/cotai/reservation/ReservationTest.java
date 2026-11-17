package dev.cotai.reservation;

import dev.cotai.event.Event;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.Item;
import dev.cotai.organizer.Organizer;
import dev.cotai.payment.Payment;
import dev.cotai.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTest {

    private final Instant now = Instant.parse("2026-11-15T12:00:00Z");
    private Item item;

    @BeforeEach
    void setUp() {
        Organizer organizer = new Organizer("Ana", "ana@example.com", "hash", now);
        GiftList list = new GiftList(organizer, new Event("BARBECUE", "Churrasco", 30), "Churrasco",
                "churrasco", LocalDate.of(2026, 12, 1), 30, now);
        item = list.addItem("Carvao", "saco", 4500);
    }

    @Test
    @DisplayName("new reservation is PENDING and copies the item price")
    void newReservationIsPending() {
        Reservation r = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1, "hash", now);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(r.getUnitPriceCents()).isEqualTo(4500);
        assertThat(r.getAmountCents()).isEqualTo(4500);
        assertThat(r.getPayment()).isNull();
    }

    private Reservation withPayment() {
        Reservation r = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1, "hash", now);
        r.setPayment(new Payment("mock", r.getAmountCents(), now.plus(Duration.ofMinutes(30))));
        return r;
    }

    @Test
    @DisplayName("effectiveStatus reports EXPIRED once a PENDING reservation passes its payment deadline")
    void effectiveStatusExpires() {
        Reservation r = withPayment();

        assertThat(r.effectiveStatus(now.plus(Duration.ofMinutes(29)))).isEqualTo(ReservationStatus.PENDING);
        assertThat(r.effectiveStatus(now.plus(Duration.ofMinutes(30)))).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("effectiveStatus keeps PENDING while there is no payment yet")
    void pendingWithoutPayment() {
        Reservation r = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1, "hash", now);

        assertThat(r.effectiveStatus(now.plus(Duration.ofHours(2)))).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("effectiveStatus keeps a CONFIRMED reservation confirmed after the deadline")
    void confirmedNeverExpires() {
        Reservation r = withPayment();
        r.setStatus(ReservationStatus.CONFIRMED);

        assertThat(r.effectiveStatus(now.plus(Duration.ofHours(2)))).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("a new payment starts CREATED and is reached from the reservation")
    void paymentStartsCreated() {
        Reservation r = withPayment();

        assertThat(r.getPayment().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(r.getPayment().getAmountCents()).isEqualTo(4500);
        assertThat(r.getPayment().getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
    }
}
