package dev.cotai.reservation;

import dev.cotai.security.AppProperties;
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
import dev.cotai.payment.PaymentInfo;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReservationView;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** Each item is one quota: it is reserved and paid as a whole. */
    private static final int QUOTAS_PER_ITEM = 1;

    private final ItemRepository items;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final AppProperties appProperties;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    @Transactional
    public ReservationDtos.ReserveOutcome reserve(Long itemId, ReserveRequest cmd, UUID idempotencyKey) {
        String manageToken = manageToken(idempotencyKey);
        String manageTokenHash = Crypto.sha256Hex(manageToken);

        var existing = reservations.findByManageTokenHash(manageTokenHash);
        if (existing.isPresent()) {
            Reservation replay = reservations.findWithDetailsById(existing.get().getId()).orElseThrow();
            return new ReservationDtos.ReserveOutcome(toCreated(replay, manageToken), true);
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

        int available = Math.max(QUOTAS_PER_ITEM - items.occupiedQuotas(itemId, now), 0);
        if (cmd.quantity() > available) {
            throw new BusinessException(ErrorCode.QUOTA_UNAVAILABLE,
                    "Only " + available + " quotas are still available for item " + itemId + ".",
                    Map.of("availableQuotas", available));
        }

        Reservation reservation = new Reservation(UUID.randomUUID(), item, cmd.guestName().trim(), email,
                cmd.quantity(), manageTokenHash, now);
        reservation.setMessage(cleanMessage(cmd.message()));
        Instant expiresAt = now.plus(Duration.ofMinutes(list.getReservationTtlMinutes()));
        reservation.setPayment(payments.save(new Payment(gateway.providerName(), reservation.getAmountCents(), expiresAt)));
        reservations.saveAndFlush(reservation);

        return new ReservationDtos.ReserveOutcome(toCreated(reservation, manageToken), false);
    }

    @Transactional(readOnly = true)
    public ReservationCreated replayByKey(UUID idempotencyKey, ReserveRequest cmd, Long itemId) {
        String manageToken = manageToken(idempotencyKey);
        String manageTokenHash = Crypto.sha256Hex(manageToken);
        Reservation existing = reservations.findByManageTokenHash(manageTokenHash)
                .flatMap(r -> reservations.findWithDetailsById(r.getId()))
                .orElseThrow(() -> new IllegalStateException("Reservation vanished for key " + idempotencyKey));
        return toCreated(existing, manageToken);
    }

    @Transactional(readOnly = true)
    public ReservationView get(UUID reservationId, String manageToken) {
        Reservation reservation = findAuthorized(reservationId, manageToken);
        return new ReservationView(reservation.getId(), reservation.getItem().getName(),
                reservation.effectiveStatus(clock.instant()), reservation.getQuantity(),
                reservation.getAmountCents(), reservation.getPayment().getExpiresAt(),
                PaymentInfo.of(reservation.getPayment()));
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

    @Transactional(readOnly = true)
    public Optional<Reservation> findByPaymentReference(String providerReference) {
        return reservations.findByPaymentProviderReference(providerReference);
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

    private static String cleanMessage(String message) {
        String trimmed = message == null ? "" : message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String manageToken(UUID idempotencyKey) {
        return Crypto.hmacSha256Base64Url(appProperties.manageTokenSecret(), idempotencyKey.toString());
    }

    private Reservation findAuthorized(UUID reservationId, String manageToken) {
        return reservations.findWithDetailsById(reservationId)
                .filter(r -> manageToken != null
                        && Crypto.constantTimeEquals(r.getManageTokenHash(), Crypto.sha256Hex(manageToken)))
                .orElseThrow(() -> BusinessException.notFound("Reservation"));
    }

    private ReservationCreated toCreated(Reservation r, String manageToken) {
        return new ReservationCreated(r.getId(), manageToken, r.getStatus(), r.getQuantity(),
                r.getUnitPriceCents(), r.getAmountCents(), r.getPayment().getExpiresAt(), PaymentInfo.of(r.getPayment()));
    }
}
