package dev.cotai.dashboard;

import dev.cotai.TestcontainersConfiguration;
import dev.cotai.common.BusinessException;
import dev.cotai.common.Crypto;
import dev.cotai.dashboard.DashboardDtos.ContributionPage;
import dev.cotai.dashboard.DashboardDtos.FundraisingMetrics;
import dev.cotai.devsupport.MutableClock;
import dev.cotai.eventtype.EventTypeRepository;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListRepository;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.payment.PaymentProperties;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import dev.cotai.reservation.ReservationFlow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@org.springframework.boot.test.context.SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class DashboardIT {

    @Autowired
    private DashboardService dashboard;
    @Autowired
    private ReservationFlow flow;
    @Autowired
    private OrganizerRepository organizers;
    @Autowired
    private EventTypeRepository events;
    @Autowired
    private GiftListRepository giftLists;
    @Autowired
    private PaymentProperties paymentProperties;
    @Autowired
    private MutableClock clock;

    private Organizer owner;
    private GiftList list;

    @BeforeEach
    void setUp() {
        owner = organizers.save(new Organizer("Ana", "ana-" + UUID.randomUUID() + "@example.com", "hash",
                clock.instant()));
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        GiftList draft = new GiftList(owner, events.findById("BARBECUE").orElseThrow(), "Churrasco",
                "churrasco-" + UUID.randomUUID(), today.plusDays(30), 30, clock.instant());
        draft.addItem("Carvao", "saco", 4500);
        draft.addItem("Picanha", "kg", 9000);
        draft.addItem("Gelo", "saco", 1000);
        draft.publish(today);
        list = giftLists.saveAndFlush(draft);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private Long itemId(int index) {
        return list.getItems().get(index).getId();
    }

    private ReservationCreated reserve(int index, String guest, String email, String message) {
        return flow.reserve(itemId(index), new ReserveRequest(guest, email, 1, message), UUID.randomUUID()).body();
    }

    private void pay(ReservationCreated created) {
        byte[] body = ("{\"eventId\":\"" + UUID.randomUUID() + "\",\"type\":\"payment.paid\","
                + "\"providerReference\":\"" + created.payment().providerReference() + "\","
                + "\"amountCents\":" + created.amountCents() + "}").getBytes(StandardCharsets.UTF_8);
        flow.handlePaymentNotification(body,
                "sha256=" + Crypto.hmacSha256Hex(paymentProperties.webhookSecret(), body));
    }

    @Test
    @DisplayName("metrics count only paid money as raised and keep unexpired pending apart")
    void metrics() {
        pay(reserve(0, "Bia", "bia@example.com", "Parabens!"));
        reserve(1, "Caio", "caio@example.com", null);

        FundraisingMetrics m = dashboard.metrics(owner.getId(), list.getId());

        assertThat(m.targetCents()).isEqualTo(14500);
        assertThat(m.confirmedCents()).isEqualTo(4500);
        assertThat(m.pendingCents()).isEqualTo(9000);
        assertThat(m.percentRaised()).isEqualTo(31);
        assertThat(m.totalQuotas()).isEqualTo(3);
        assertThat(m.paidQuotas()).isEqualTo(1);
        assertThat(m.pendingQuotas()).isEqualTo(1);
        assertThat(m.remainingQuotas()).isEqualTo(1);
        assertThat(m.contributions()).isEqualTo(1);
        assertThat(m.contributors()).isEqualTo(1);
        assertThat(m.lastContributionAt()).isNotNull();
        assertThat(m.items()).extracting(i -> i.name() + ":" + i.paidQuotas() + "/" + i.remainingQuotas())
                .containsExactly("Carvao:1/0", "Picanha:0/0", "Gelo:0/1");
    }

    @Test
    @DisplayName("a pending reservation that expired is released and not counted")
    void expiredPendingIsNotCounted() {
        reserve(1, "Caio", "caio@example.com", null);

        clock.advance(Duration.ofMinutes(31));
        FundraisingMetrics m = dashboard.metrics(owner.getId(), list.getId());

        assertThat(m.pendingCents()).isZero();
        assertThat(m.remainingQuotas()).isEqualTo(3);
    }

    @Test
    @DisplayName("contributions list only paid reservations, newest first, with the guest message")
    void contributionsFeed() {
        pay(reserve(0, "Bia", "bia@example.com", "  Parabens!  "));
        clock.advance(Duration.ofMinutes(1));
        pay(reserve(2, "Dani", "dani@example.com", "   "));
        reserve(1, "Caio", "caio@example.com", "ainda nao paguei");

        ContributionPage page = dashboard.contributions(owner.getId(), list.getId(), null, 0, 10);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(c -> c.guestName()).containsExactly("Dani", "Bia");
        assertThat(page.content().get(0).message()).isNull();
        assertThat(page.content().get(1).message()).isEqualTo("Parabens!");
        assertThat(page.content().get(1).amountCents()).isEqualTo(4500);
        assertThat(page.content().get(1).itemName()).isEqualTo("Carvao");
    }

    @Test
    @DisplayName("contributions paginate and can be filtered by item")
    void paginationAndItemFilter() {
        pay(reserve(0, "Bia", "bia@example.com", null));
        pay(reserve(2, "Dani", "dani@example.com", null));

        ContributionPage first = dashboard.contributions(owner.getId(), list.getId(), null, 0, 1);
        assertThat(first.content()).hasSize(1);
        assertThat(first.totalPages()).isEqualTo(2);

        ContributionPage byItem = dashboard.contributions(owner.getId(), list.getId(), itemId(2), 0, 10);
        assertThat(byItem.content()).extracting(c -> c.guestName()).containsExactly("Dani");

        ContributionPage capped = dashboard.contributions(owner.getId(), list.getId(), null, -3, 1000);
        assertThat(capped.page()).isZero();
        assertThat(capped.size()).isEqualTo(DashboardService.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("another organizer gets not found, and so does an item from another list")
    void ownership() {
        Organizer stranger = organizers.save(new Organizer("Eve", "eve-" + UUID.randomUUID() + "@example.com",
                "hash", clock.instant()));

        assertThatThrownBy(() -> dashboard.metrics(stranger.getId(), list.getId()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> dashboard.contributions(stranger.getId(), list.getId(), null, 0, 10))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> dashboard.contributions(owner.getId(), list.getId(), -1L, 0, 10))
                .isInstanceOf(BusinessException.class);
    }
}
