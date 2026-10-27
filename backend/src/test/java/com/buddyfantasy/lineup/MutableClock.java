package com.buddyfantasy.lineup;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** The server clock of the lineup tests: each test sets {@link #now}. Import {@link Config} to use it. */
class MutableClock extends Clock {

    Instant now = Instant.parse("2026-01-10T10:00:00Z");

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }
}
