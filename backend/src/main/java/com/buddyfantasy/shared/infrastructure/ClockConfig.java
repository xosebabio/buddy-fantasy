package com.buddyfantasy.shared.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** The server clock: the real one, or with the {@code e2e} profile one the E2E tests move forward. */
@Configuration
class ClockConfig {

    @Bean
    @Profile("!" + E2eClock.PROFILE)
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @Profile(E2eClock.PROFILE)
    E2eClock e2eClock() {
        return new E2eClock(Clock.systemUTC());
    }
}
