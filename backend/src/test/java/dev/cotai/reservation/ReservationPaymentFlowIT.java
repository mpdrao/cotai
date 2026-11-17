package dev.cotai.reservation;

import dev.cotai.TestcontainersConfiguration;
import dev.cotai.common.Crypto;
import dev.cotai.devsupport.MutableClock;
import dev.cotai.event.EventRepository;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListRepository;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.payment.PaymentProperties;
import dev.cotai.payment.PaymentRepository;
import dev.cotai.payment.PaymentStatus;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReservationView;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** US-03 Reservar Cotas + US-04 Processar Pagamento against a real PostgreSQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class ReservationPaymentFlowIT {

    @Autowired
    private ReservationFlow flow;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private PaymentRepository payments;
    @Autowired
    private OrganizerRepository organizers;
    @Autowired
    private EventRepository events;
    @Autowired
    private GiftListRepository giftLists;
    @Autowired
    private PaymentProperties paymentProperties;
    @Autowired
    private MutableClock clock;

    private GiftList list;

    @BeforeEach
    void setUp() {
        Organizer owner = organizers.save(new Organizer("Ana", "ana-" + UUID.randomUUID() + "@example.com",
                "hash", clock.instant()));
        LocalDate today = LocalDate.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        GiftList draft = new GiftList(owner, events.findById("BARBECUE").orElseThrow(), "Churrasco",
                "churrasco-" + UUID.randomUUID(), today.plusDays(30), 30, clock.instant());
        draft.addItem("Carvao", "saco", 4500);
        draft.addItem("Picanha", "kg", 9000);
        draft.publish(today);
        list = giftLists.saveAndFlush(draft);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private ReservationCreated reserve(int itemIndex) {
        Long itemId = list.getItems().get(itemIndex).getId();
        return flow.reserve(itemId, new ReserveRequest("Bia", "bia@example.com", 1), UUID.randomUUID()).body();
    }

    private void notifyPaid(ReservationCreated created) {
        byte[] body = ("{\"eventId\":\"" + UUID.randomUUID() + "\",\"type\":\"payment.paid\","
                + "\"providerReference\":\"" + created.payment().providerReference() + "\","
                + "\"amountCents\":" + created.amountCents() + "}").getBytes(StandardCharsets.UTF_8);
        flow.handlePaymentNotification(body,
                "sha256=" + Crypto.hmacSha256Hex(paymentProperties.webhookSecret(), body));
    }

    @Test
    @DisplayName("guest reserves, gets a Pix charge and the paid webhook confirms the reservation")
    void reserveAndPayOnTime() {
        ReservationCreated created = reserve(0);

        assertThat(created.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(created.payment().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(created.payment().qrCodePayload()).isNotBlank();

        notifyPaid(created);

        ReservationView view = flow.get(created.reservationId(), created.manageToken());
        assertThat(view.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(view.payment().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(payments.sumLatePayments(list.getId())).isZero();
    }

    @Test
    @DisplayName("a payment after the deadline is recorded as PAID but does not reopen the reservation")
    void payAfterDeadline() {
        ReservationCreated created = reserve(1);

        clock.advance(Duration.ofMinutes(31));
        reservationService.expireOverdue();
        notifyPaid(created);

        ReservationView view = flow.get(created.reservationId(), created.manageToken());
        assertThat(view.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(view.payment().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(payments.sumLatePayments(list.getId())).isEqualTo(9000);
    }
}
