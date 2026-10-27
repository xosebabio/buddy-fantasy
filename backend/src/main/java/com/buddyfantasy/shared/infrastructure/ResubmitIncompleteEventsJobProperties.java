package com.buddyfantasy.shared.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Technical configuration of the job that resubmits the incomplete event publications. It only resubmits those
 * published at least {@code minAge} ago, so a listener still running is not called twice at once. Its schedule is
 * {@code shared.events.resubmit-job.cron}, read by {@code @Scheduled}.
 */
@ConfigurationProperties("shared.events.resubmit-job")
record ResubmitIncompleteEventsJobProperties(Duration minAge) {

    ResubmitIncompleteEventsJobProperties {
        if (minAge == null || minAge.isNegative() || minAge.isZero()) {
            throw new IllegalArgumentException("shared.events.resubmit-job.min-age must be > 0");
        }
    }
}
