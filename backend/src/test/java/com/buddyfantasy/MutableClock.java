package com.buddyfantasy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A server clock the test moves by hand: each test sets {@link #now}. Import {@link Config} to use it. For a race
 * between requests that read the clock at different times, {@link #at} sets it for one thread only.
 */
public class MutableClock extends Clock {

    public Instant now = Instant.parse("2026-01-10T10:00:00Z");

    private final ThreadLocal<Instant> threadNow = new ThreadLocal<>();

    /** Runs {@code task} with the clock at {@code at} on this thread; the others still see {@link #now}. */
    public <T> T at(Instant at, Callable<T> task) throws Exception {
        threadNow.set(at);
        try {
            return task.call();
        } finally {
            threadNow.remove();
        }
    }

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
        Instant thread = threadNow.get();
        return thread != null ? thread : now;
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
