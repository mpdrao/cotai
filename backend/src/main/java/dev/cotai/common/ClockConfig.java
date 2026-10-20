package dev.cotai.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/**
 * The application never calls {@code Instant.now()} or {@code CURRENT_TIMESTAMP}: every "now" comes
 * from this {@link Clock}. In the {@code dev} profile it is replaced by
 * {@link dev.cotai.devsupport.MutableClock} so expirations can be reproduced on demand.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    @Profile("!dev")
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
