package com.buddyfantasy.shared.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * FE-34: the server clock with the {@code e2e} profile, which production never activates. It starts at the real time
 * and only moves forward ({@link E2eClockController}), so the E2E tests reach the lock of the lineups and the close of
 * the rating windows without waiting; the jobs, with a short cron in that profile, find on their own what is due.
 */
class E2eClock extends Clock {

    static final String PROFILE = "e2e";

    private final Clock base;
    private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

    E2eClock(Clock base) {
        this.base = base;
    }

    /** Moves the clock forward by {@code by}, which must be positive, and returns the new time. */
    Instant advance(Duration by) {
        if (by.isNegative() || by.isZero()) {
            throw new DomainValidationException("The E2E clock only moves forward: " + by);
        }
        offset.accumulateAndGet(by, Duration::plus);
        return instant();
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
        return base.instant().plus(offset.get());
    }
}
