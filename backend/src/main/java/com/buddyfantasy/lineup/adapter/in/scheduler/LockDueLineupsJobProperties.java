package com.buddyfantasy.lineup.adapter.in.scheduler;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Technical configuration of the lineup lock job. It only locks matches whose lock time is at least {@code margin}
 * ago, so a save made just before the lock has committed when the lineups are read. Its schedule is
 * {@code lineup.lock-job.cron}, read by {@code @Scheduled}.
 */
@ConfigurationProperties("lineup.lock-job")
record LockDueLineupsJobProperties(Duration margin) {

    LockDueLineupsJobProperties {
        if (margin == null || margin.isNegative() || margin.isZero()) {
            throw new IllegalArgumentException("lineup.lock-job.margin must be > 0");
        }
    }
}
