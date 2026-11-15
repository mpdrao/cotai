package dev.cotai.reservation;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.eventtype.Event;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.Item;
import dev.cotai.organizer.Organizer;
import dev.cotai.payment.Payment;
import dev.cotai.payment.PaymentService;
import dev.cotai.payment.PaymentService.NotificationPayload;
import dev.cotai.payment.PaymentStatus;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReserveOutcome;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationFlowTest {

    @Mock
    private ReservationService reservations;
    @Mock
    private PaymentService payments;

    private ReservationFlow flow;
    private final Instant now = Instant.parse("2026-11-15T12:00:00Z");
    private final byte[] body = "{}".getBytes();
    private Reservation reservation;
    private Payment payment;

    @BeforeEach
    void setUp() {
        flow = new ReservationFlow(reservations, payments);
        Organizer organizer = new Organizer("Ana", "ana@example.com", "hash", now);
        GiftList list = new GiftList(organizer, new Event("BARBECUE", "Churrasco", 30), "Churrasco",
                "churrasco", LocalDate.of(2026, 12, 1), 30, now);
        Item item = list.addItem("Carvao", "saco", 4500);
        reservation = new Reservation(UUID.randomUUID(), item, "Bia", "bia@example.com", 1, "hash", now);
        payment = new Payment("mock", 4500, now.plusSeconds(1800));
        payment.setProviderReference("ref-1");
        payment.setStatus(PaymentStatus.PENDING);
        reservation.setPayment(payment);
    }

    private void notification(String type, long amountCents) {
        when(payments.verifyNotification(any(), anyString()))
                .thenReturn(Optional.of(new NotificationPayload("evt-1", type, "ref-1", amountCents, now)));
    }

    @Test
    @DisplayName("a gateway failure cancels the new reservation")
    void gatewayFailureCancelsReservation() {
        ReservationCreated created = new ReservationCreated(reservation.getId(), "token",
                ReservationStatus.PENDING, 1, 4500, 4500, now, null);
        when(reservations.reserve(any(), any(), any())).thenReturn(new ReserveOutcome(created, false));
        when(payments.openCharge(reservation.getId())).thenThrow(
                new BusinessException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE, "down"));

        assertThatThrownBy(() -> flow.reserve(10L, new ReserveRequest("Bia", "bia@example.com", 1), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
        verify(reservations).cancelDeclined(reservation.getId());
    }

    @Test
    @DisplayName("payment.paid on time confirms the reservation and marks the payment PAID")
    void paidOnTime() {
        notification("payment.paid", 4500);
        when(reservations.findByPaymentReference("ref-1")).thenReturn(Optional.of(reservation));
        when(reservations.confirmPaid(reservation.getId())).thenReturn(true);

        flow.handlePaymentNotification(body, "sha256=ok");

        verify(reservations).confirmPaid(reservation.getId());
        verify(payments).markPaid(payment.getId());
    }

    @Test
    @DisplayName("late payment.paid marks the payment PAID without reopening the reservation")
    void paidLate() {
        notification("payment.paid", 4500);
        when(reservations.findByPaymentReference("ref-1")).thenReturn(Optional.of(reservation));
        when(reservations.confirmPaid(reservation.getId())).thenReturn(false);

        flow.handlePaymentNotification(body, "sha256=ok");

        verify(payments).markPaid(payment.getId());
        verify(reservations, never()).cancelDeclined(any());
    }

    @Test
    @DisplayName("payment.declined declines the payment and cancels the reservation")
    void declined() {
        notification("payment.declined", 4500);
        when(reservations.findByPaymentReference("ref-1")).thenReturn(Optional.of(reservation));

        flow.handlePaymentNotification(body, "sha256=ok");

        verify(payments).decline(payment.getId());
        verify(reservations).cancelDeclined(reservation.getId());
    }

    @Test
    @DisplayName("a duplicate delivery changes nothing")
    void duplicate() {
        when(payments.verifyNotification(any(), anyString())).thenReturn(Optional.empty());

        flow.handlePaymentNotification(body, "sha256=ok");

        verify(reservations, never()).findByPaymentReference(any());
    }

    @Test
    @DisplayName("a notification with another amount is ignored")
    void amountMismatch() {
        notification("payment.paid", 100);
        when(reservations.findByPaymentReference("ref-1")).thenReturn(Optional.of(reservation));

        flow.handlePaymentNotification(body, "sha256=ok");

        verify(reservations, never()).confirmPaid(any());
        verify(payments, never()).markPaid(any());
    }

    @Test
    @DisplayName("an already PAID payment is not paid again")
    void alreadyPaid() {
        payment.setStatus(PaymentStatus.PAID);
        notification("payment.paid", 4500);
        when(reservations.findByPaymentReference("ref-1")).thenReturn(Optional.of(reservation));

        flow.handlePaymentNotification(body, "sha256=ok");

        verify(reservations, never()).confirmPaid(any());
        verify(payments, never()).markPaid(any());
    }
}
