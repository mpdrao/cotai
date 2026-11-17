package dev.cotai.payment;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.payment.PaymentGateway.Charge;
import dev.cotai.payment.PaymentGateway.ChargeRequest;
import dev.cotai.payment.PaymentService.NotificationPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGateway gateway;
    @Mock
    private PaymentRepository payments;

    private PaymentService service;
    private final Instant now = Instant.parse("2026-11-15T12:00:00Z");
    private final UUID reservationId = UUID.randomUUID();
    private Payment payment;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new PaymentService(gateway, payments, tx, JsonMapper.builder().build(),
                Clock.fixed(now, ZoneOffset.UTC));
        payment = new Payment("mock", 4500, now.plusSeconds(1800));
    }

    private byte[] body(String eventId) {
        return ("{\"eventId\":\"" + eventId + "\",\"type\":\"payment.paid\",\"providerReference\":\"ref-1\","
                + "\"amountCents\":4500}").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("openCharge sends amount, deadline and description to the gateway and stores the Pix data")
    void openChargeStoresPixData() {
        when(payments.findByReservationId(reservationId)).thenReturn(Optional.of(payment));
        when(payments.chargeDescription(reservationId)).thenReturn("Carvao x1 - Churrasco");
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(gateway.createCharge(any(ChargeRequest.class)))
                .thenReturn(new Charge("ref-1", "http://pay/ref-1", "00020126pix"));

        PaymentInfo info = service.openCharge(reservationId);

        ArgumentCaptor<ChargeRequest> sent = ArgumentCaptor.forClass(ChargeRequest.class);
        verify(gateway).createCharge(sent.capture());
        assertThat(sent.getValue().amountCents()).isEqualTo(4500);
        assertThat(sent.getValue().expiresAt()).isEqualTo(payment.getExpiresAt());
        assertThat(sent.getValue().description()).isEqualTo("Carvao x1 - Churrasco");
        verify(payments).markPending(payment.getId(), "ref-1", "http://pay/ref-1", "00020126pix");
        assertThat(info.paymentId()).isEqualTo(payment.getId());
    }

    @Test
    @DisplayName("openCharge marks the payment FAILED when the gateway is down")
    void openChargeGatewayDown() {
        when(payments.findByReservationId(reservationId)).thenReturn(Optional.of(payment));
        when(gateway.createCharge(any(ChargeRequest.class))).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.openCharge(reservationId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
        verify(payments).transition(payment.getId(), PaymentStatus.CREATED, PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("verifyNotification returns the payload once and empty for the same event again")
    void verifyNotificationDeduplicates() {
        when(gateway.isSignatureValid(any(), anyString())).thenReturn(true);

        Optional<NotificationPayload> first = service.verifyNotification(body("evt-1"), "sha256=ok");
        Optional<NotificationPayload> again = service.verifyNotification(body("evt-1"), "sha256=ok");

        assertThat(first).isPresent();
        assertThat(first.get().providerReference()).isEqualTo("ref-1");
        assertThat(again).isEmpty();
    }

    @Test
    @DisplayName("verifyNotification refuses an invalid signature before reading the body")
    void invalidSignature() {
        when(gateway.isSignatureValid(any(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyNotification(body("evt-1"), "bad"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("verifyNotification refuses a body without the required fields")
    void invalidBody() {
        when(gateway.isSignatureValid(any(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.verifyNotification("{}".getBytes(StandardCharsets.UTF_8), "sha256=ok"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("markPaid and decline are conditional updates on the payment")
    void markPaidAndDecline() {
        service.markPaid(payment.getId());
        service.decline(payment.getId());

        verify(payments).markPaid(payment.getId(), now);
        verify(payments).transition(payment.getId(), PaymentStatus.PENDING, PaymentStatus.DECLINED);
        verify(payments, never()).save(any());
    }
}
