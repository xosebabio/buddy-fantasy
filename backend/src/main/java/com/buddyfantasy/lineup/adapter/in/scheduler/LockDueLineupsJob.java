package com.buddyfantasy.lineup.adapter.in.scheduler;

import com.buddyfantasy.lineup.application.usecases.findmatchestolock.FindMatchesToLockQuery;
import com.buddyfantasy.lineup.application.usecases.findmatchestolock.MatchToLock;
import com.buddyfantasy.lineup.application.usecases.lockmatchlineups.LockMatchLineupsCommand;
import com.buddyfantasy.shared.application.CommandBus;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Locks the lineups of every match whose lock time has come (RF-25, RF-27), one transaction per match so a failure
 * does not hold the others back. Idempotent: running it twice locks and publishes nothing more.
 */
@Component
@EnableConfigurationProperties(LockDueLineupsJobProperties.class)
public class LockDueLineupsJob {

    private static final Logger log = LoggerFactory.getLogger(LockDueLineupsJob.class);

    private final CommandBus bus;
    private final LockDueLineupsJobProperties properties;

    LockDueLineupsJob(CommandBus bus, LockDueLineupsJobProperties properties) {
        this.bus = bus;
        this.properties = properties;
    }

    @Scheduled(cron = "${lineup.lock-job.cron}")
    @SchedulerLock(name = "LockDueLineupsJob")
    public void lockDueLineups() {
        for (MatchToLock match : bus.ask(new FindMatchesToLockQuery(properties.margin()))) {
            try {
                bus.dispatch(new LockMatchLineupsCommand(match.leagueId(), match.matchId()));
            } catch (RuntimeException e) {
                // retried on the next run; MatchClosed is the last safety net
                log.error("Could not lock the lineups of match {}", match.matchId(), e);
            }
        }
    }
}
