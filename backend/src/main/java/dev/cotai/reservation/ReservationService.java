package dev.cotai.reservation;

import dev.cotai.common.AppProperties;
import dev.cotai.common.BusinessException;
import dev.cotai.common.Crypto;
import dev.cotai.common.ErrorCode;
import dev.cotai.giftlist.GiftList;
import dev.cotai.giftlist.GiftListStatus;
import dev.cotai.giftlist.Item;
import dev.cotai.giftlist.ItemRepository;
import dev.cotai.payment.Payment;
import dev.cotai.payment.PaymentGateway;
import dev.cotai.payment.PaymentProperties;
import dev.cotai.payment.PaymentRepository;
import dev.cotai.reservation.dto.PaymentInfo;
import dev.cotai.reservation.dto.ReservationCreated;
import dev.cotai.reservation.dto.ReservationView;
import dev.cotai.reservation.dto.ReserveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ItemRepository items;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final AppProperties appProperties;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    @Transactional
    public ReserveOutcome reserve(Long itemId, ReserveRequest cmd, UUID idempotencyKey) {
        String requestHash = Crypto.sha256Hex(itemId + "|" + cmd.guestName().trim() + "|"
                + cmd.guestEmail().trim().toLowerCase() + "|" + cmd.quantity());

        var existing = reservations.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new ReserveOutcome(replay(existing.get(), requestHash), true);
        }

        Instant now = clock.instant();
        Item item = items.lockById(itemId).orElseThrow(() -> BusinessException.notFound("Item"));
        GiftList list = item.getGiftList();
        if (item.isArchived() || list.getStatus() == GiftListStatus.DRAFT) {
            throw BusinessException.notFound("Item");
        }

        LocalDate today = LocalDate.ofInstant(now, appProperties.zone());
        if (!list.acceptsReservations(today)) {
            throw new BusinessException(ErrorCode.LIST_NOT_ACCEPTING_RESERVATIONS,
                    "This gift list is not accepting reservations.");
        }

        String email = cmd.guestEmail().trim().toLowerCase();
        if (reservations.countActivePendingByEmail(list.getId(), email, now)
                >= paymentProperties.maxPendingReservationsPerEmail()) {
            throw new BusinessException(ErrorCode.TOO_MANY_PENDING_RESERVATIONS,
                    "Too many pending reservations for this e-mail. Pay or cancel one first.");
        }

        int available = item.getTotalQuotas() - items.occupiedQuotas(itemId, now);
        if (cmd.quantity() > available) {
            throw new BusinessException(ErrorCode.QUOTA_UNAVAILABLE,
                    "Only " + Math.max(available, 0) + " of " + item.getTotalQuotas()
                            + " quotas are still available for item " + itemId + ".",
                    Map.of("availableQuotas", Math.max(available, 0)));
        }

        UUID reservationId = UUID.randomUUID();
        String manageToken = manageToken(idempotencyKey);
        Reservation reservation = reservations.saveAndFlush(new Reservation(
                reservationId, item, cmd.guestName().trim(), email, cmd.quantity(),
                Crypto.sha256Hex(manageToken), idempotencyKey, requestHash,
                now, now.plus(list.getReservationTtlMinutes(), ChronoUnit.MINUTES)));
        Payment payment = payments.save(new Payment(reservation, gateway.providerName()));

        return new ReserveOutcome(toCreated(reservation, manageToken, payment), false);
    }

    @Transactional(readOnly = true)
    public ReservationCreated replayByKey(UUID idempotencyKey, ReserveRequest cmd, Long itemId) {
        String requestHash = Crypto.sha256Hex(itemId + "|" + cmd.guestName().trim() + "|"
                + cmd.guestEmail().trim().toLowerCase() + "|" + cmd.quantity());
        Reservation existing = reservations.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Reservation vanished for key " + idempotencyKey));
        return replay(existing, requestHash);
    }

    @Transactional(readOnly = true)
    public ReservationView get(UUID reservationId, String manageToken) {
        Reservation reservation = findAuthorized(reservationId, manageToken);
        Payment payment = payments.findByReservationIdWithDetails(reservationId).orElseThrow();
        return new ReservationView(reservation.getId(), reservation.getItem().getName(),
                reservation.effectiveStatus(clock.instant()), reservation.getQuantity(),
                reservation.getAmountCents(), reservation.getExpiresAt(), toInfo(payment));
    }

    @Transactional
    public void cancel(UUID reservationId, String manageToken) {
        Reservation reservation = findAuthorized(reservationId, manageToken);
        Instant now = clock.instant();
        if (reservation.effectiveStatus(now) != ReservationStatus.PENDING
                || reservations.markCancelled(reservationId, now) == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "Only PENDING reservations can be cancelled.");
        }
    }

    @Transactional
    public boolean confirmPaid(UUID reservationId) {
        return reservations.markConfirmed(reservationId, clock.instant()) == 1;
    }

    @Transactional
    public boolean cancelDeclined(UUID reservationId) {
        return reservations.markCancelled(reservationId, clock.instant()) == 1;
    }

    @Transactional
    public int expireOverdue() {
        int expired = reservations.markExpiredOverdue(clock.instant());
        payments.expireForExpiredReservations();
        return expired;
    }

    private String manageToken(UUID idempotencyKey) {
        return Crypto.hmacSha256Base64Url(appProperties.manageTokenSecret(), idempotencyKey.toString());
    }

    private ReservationCreated replay(Reservation existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_MISMATCH,
                    "This Idempotency-Key was already used with a different request body.");
        }
        Payment payment = payments.findByReservationIdWithDetails(existing.getId()).orElseThrow();
        return toCreated(existing, manageToken(existing.getIdempotencyKey()), payment);
    }

    private Reservation findAuthorized(UUID reservationId, String manageToken) {

        return reservations.findById(reservationId)
                .filter(r -> manageToken != null
                        && Crypto.constantTimeEquals(r.getManageTokenHash(), Crypto.sha256Hex(manageToken)))
                .orElseThrow(() -> BusinessException.notFound("Reservation"));
    }

    private ReservationCreated toCreated(Reservation r, String manageToken, Payment p) {
        return new ReservationCreated(r.getId(), manageToken, r.getStatus(), r.getQuantity(),
                r.getUnitPriceCents(), r.getAmountCents(), r.getExpiresAt(), toInfo(p));
    }

    public static PaymentInfo toInfo(Payment p) {
        return new PaymentInfo(p.getId(), p.getStatus(), p.getAmountCents(), p.getProviderReference(),
                p.getCheckoutUrl(), p.getQrCodePayload(), p.getExpiresAt());
    }
}
