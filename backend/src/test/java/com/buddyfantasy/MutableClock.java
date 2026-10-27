package com.buddyfantasy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** A server clock the test moves by hand: each test sets {@link #now}. Import {@link Config} to use it. */
public class MutableClock extends Clock {

    public Instant now = Instant.parse("2026-01-10T10:00:00Z");

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
    public static class Config {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }
}
