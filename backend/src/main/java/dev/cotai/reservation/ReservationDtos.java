package dev.cotai.reservation;

import dev.cotai.payment.PaymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** Guest-facing request/response records. */
public final class ReservationDtos {

    private ReservationDtos() {
    }

    public record ReserveRequest(
            @NotBlank @Size(min = 2, max = 80) String guestName,
            @NotBlank @Email @Size(max = 254) String guestEmail,
            @Min(1) @Max(1000) int quantity) {
    }

    public record PaymentInfo(UUID paymentId, PaymentStatus status, long amountCents, String providerReference,
                              String checkoutUrl, String qrCodePayload, Instant expiresAt) {
    }

    public record ReservationCreated(UUID reservationId, String manageToken, ReservationStatus status, int quantity,
                                     long unitPriceCents, long amountCents, Instant expiresAt, PaymentInfo payment) {

        public ReservationCreated withPayment(PaymentInfo newPayment) {
            return new ReservationCreated(reservationId, manageToken, status, quantity, unitPriceCents,
                    amountCents, expiresAt, newPayment);
        }
    }

    public record ReservationView(UUID reservationId, String itemName, ReservationStatus status, int quantity,
                                  long amountCents, Instant expiresAt, PaymentInfo payment) {
    }

    /** Result of {@code reserve}: {@code replay} is true when the Idempotency-Key was already used. */
    public record ReserveOutcome(ReservationCreated body, boolean replay) {
    }
}
