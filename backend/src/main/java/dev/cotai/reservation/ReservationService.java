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
import dev.cotai.reservation.ReservationDtos.PaymentInfo;
import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReservationView;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    public ReservationDtos.ReserveOutcome reserve(Long itemId, ReserveRequest cmd, UUID idempotencyKey) {
        String manageToken = manageToken(idempotencyKey);
        String manageTokenHash = Crypto.sha256Hex(manageToken);

        var existing = reservations.findByManageTokenHash(manageTokenHash);
        if (existing.isPresent()) {
            Payment p = payments.findByReservationIdWithDetails(existing.get().getId()).orElseThrow();
            return new ReservationDtos.ReserveOutcome(toCreated(existing.get(), manageToken, p), true);
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

        int occupied = items.occupiedQuotas(itemId, now);
        int available = Math.max(1 - occupied, 0);
        if (cmd.quantity() > available && occupied > 0) {
            throw new BusinessException(ErrorCode.QUOTA_UNAVAILABLE,
                    "Only " + available + " quotas are still available for item " + itemId + ".",
                    Map.of("availableQuotas", available));
        }

        UUID reservationId = UUID.randomUUID();
        Reservation reservation = reservations.saveAndFlush(new Reservation(
                reservationId, item, cmd.guestName().trim(), email, cmd.quantity(),
                manageTokenHash, now));
        Payment payment = payments.save(new Payment(reservation, gateway.providerName()));

        return new ReservationDtos.ReserveOutcome(toCreated(reservation, manageToken, payment), false);
    }

    @Transactional(readOnly = true)
    public ReservationCreated replayByKey(UUID idempotencyKey, ReserveRequest cmd, Long itemId) {
        String manageToken = manageToken(idempotencyKey);
        String manageTokenHash = Crypto.sha256Hex(manageToken);
        Reservation existing = reservations.findByManageTokenHash(manageTokenHash)
                .orElseThrow(() -> new IllegalStateException("Reservation vanished for key " + idempotencyKey));
        Payment payment = payments.findByReservationIdWithDetails(existing.getId()).orElseThrow();
        return toCreated(existing, manageToken, payment);
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
