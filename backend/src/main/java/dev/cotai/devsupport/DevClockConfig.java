package dev.cotai.devsupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DevClockConfig {

    @Bean
    public MutableClock mutableClock() {
        return new MutableClock(Clock.systemUTC());
    }
}
