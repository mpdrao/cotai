package dev.cotai.reservation;

import dev.cotai.reservation.ReservationDtos.ReservationCreated;
import dev.cotai.reservation.ReservationDtos.ReservationView;
import dev.cotai.reservation.ReservationDtos.ReserveOutcome;
import dev.cotai.reservation.ReservationDtos.ReserveRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Guest area. No login: reservations are managed with the {@code X-Manage-Token} header. */
@RestController
@RequestMapping("/api/public")
public class PublicReservationController {

    private final ReservationFlow flow;
    private final ReservationService reservations;

    public PublicReservationController(ReservationFlow flow, ReservationService reservations) {
        this.flow = flow;
        this.reservations = reservations;
    }

    @PostMapping("/items/{itemId}/reservations")
    public ResponseEntity<ReservationCreated> reserve(@PathVariable Long itemId,
                                                      @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                                      @Valid @RequestBody ReserveRequest body) {
        ReserveOutcome outcome = flow.reserve(itemId, body, idempotencyKey);
        if (outcome.replay()) {
            return ResponseEntity.ok(outcome.body());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/public/reservations/" + outcome.body().reservationId()))
                .body(outcome.body());
    }

    @GetMapping("/reservations/{reservationId}")
    public ReservationView get(@PathVariable UUID reservationId,
                               @RequestHeader("X-Manage-Token") String manageToken) {
        return reservations.get(reservationId, manageToken);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID reservationId,
                                       @RequestHeader("X-Manage-Token") String manageToken) {
        reservations.cancel(reservationId, manageToken);
        return ResponseEntity.noContent().build();
    }
}
