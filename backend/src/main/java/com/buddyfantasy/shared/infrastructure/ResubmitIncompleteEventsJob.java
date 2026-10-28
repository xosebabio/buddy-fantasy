package com.buddyfantasy.shared.infrastructure;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Delivers again the module events whose listener failed (RNF-03), so a transient failure does not lose them: the
 * publication stays incomplete until a delivery succeeds, and the ones still failing are retried on every run.
 * Delivery is therefore at least once, and every listener must be idempotent. The incomplete publications are also
 * resubmitted on start ({@code spring.modulith.events.republish-outstanding-events-on-restart}).
 */
@Component
@EnableConfigurationProperties(ResubmitIncompleteEventsJobProperties.class)
@RequiredArgsConstructor
public class ResubmitIncompleteEventsJob {

    private final IncompleteEventPublications publications;
    private final ResubmitIncompleteEventsJobProperties properties;

    @Scheduled(cron = "${shared.events.resubmit-job.cron}")
    @SchedulerLock(name = "ResubmitIncompleteEventsJob")
    public void resubmitIncompleteEvents() {
        publications.resubmitIncompletePublicationsOlderThan(properties.minAge());
    }
}
