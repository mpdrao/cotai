package dev.cotai.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    @Profile("!dev")
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
