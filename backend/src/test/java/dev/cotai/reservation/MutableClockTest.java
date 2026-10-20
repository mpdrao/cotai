package dev.cotai.reservation;

import dev.cotai.devsupport.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MutableClockTest {

    @Test
    void advanceMovesNowAndResetRestoresIt() {
        Instant fixed = Instant.parse("2026-12-01T01:30:00Z");
        MutableClock clock = new MutableClock(Clock.fixed(fixed, ZoneOffset.UTC));

        clock.advance(Duration.ofMinutes(31));
        assertThat(clock.instant()).isEqualTo(fixed.plus(Duration.ofMinutes(31)));
        assertThat(clock.offsetMinutes()).isEqualTo(31);

        clock.reset();
        assertThat(clock.instant()).isEqualTo(fixed);
    }
}
