package dev.cotai.devsupport;

import dev.cotai.reservation.ReservationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Controllable clock and job triggers (dev profile only). */
@Profile("dev")
@RestController
@RequestMapping("/dev")
@Validated
@RequiredArgsConstructor
public class DevController {

    public record DevClock(Instant now, long offsetMinutes) {
    }

    public record AdvanceRequest(@Min(1) @Max(100000) int minutes) {
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("dev")
    static class DevClockConfig {
        @Bean
        MutableClock mutableClock() {
            return new MutableClock(Clock.systemUTC());
        }
    }

    private final MutableClock clock;
    private final ReservationService reservations;

    @GetMapping("/clock")
    public DevClock get() {
        return snapshot();
    }

    @PostMapping("/clock/advance")
    public DevClock advance(@RequestBody @jakarta.validation.Valid AdvanceRequest body) {
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
