package dev.cotai.devsupport;

import dev.cotai.devsupport.dto.AdvanceRequest;
import dev.cotai.devsupport.dto.DevClock;
import dev.cotai.reservation.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/dev")
@Validated
@RequiredArgsConstructor
public class DevController {

    private final MutableClock clock;
    private final ReservationService reservations;

    @GetMapping("/clock")
    public DevClock get() {
        return snapshot();
    }

    @PostMapping("/clock/advance")
    public DevClock advance(@RequestBody @Valid AdvanceRequest body) {
        clock.advance(Duration.ofMinutes(body.minutes()));
        return snapshot();
    }

    @PostMapping("/clock/reset")
    public DevClock reset() {
        clock.reset();
        return snapshot();
    }

    @PostMapping("/jobs/expire-reservations")
    public Map<String, Integer> expireReservations() {
        return Map.of("expiredReservations", reservations.expireOverdue());
    }

    private DevClock snapshot() {
        return new DevClock(clock.instant(), clock.offsetMinutes());
    }
}
