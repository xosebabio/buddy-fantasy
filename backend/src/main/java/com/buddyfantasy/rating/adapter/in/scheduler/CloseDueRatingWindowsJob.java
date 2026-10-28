package com.buddyfantasy.rating.adapter.in.scheduler;

import java.util.UUID;

import com.buddyfantasy.rating.application.usecases.closeratingwindow.CloseRatingWindowCommand;
import com.buddyfantasy.rating.application.usecases.finddueratingwindows.FindDueRatingWindowsQuery;
import com.buddyfantasy.shared.application.CommandBus;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes every rating window whose time is up (RF-17), one transaction per match so a failure does not hold the others
 * back. No margin after {@code closesAt}: the lock on the window's row already orders the votes and the close.
 * Idempotent: running it twice closes and publishes nothing more.
 */
@Component
public class CloseDueRatingWindowsJob {

    private static final Logger log = LoggerFactory.getLogger(CloseDueRatingWindowsJob.class);

    private final CommandBus bus;

    CloseDueRatingWindowsJob(CommandBus bus) {
        this.bus = bus;
    }

    @Scheduled(cron = "${rating.close-job.cron}")
    @SchedulerLock(name = "CloseDueRatingWindowsJob")
    public void closeDueRatingWindows() {
        for (UUID matchId : bus.ask(new FindDueRatingWindowsQuery())) {
            try {
                bus.dispatch(new CloseRatingWindowCommand(matchId));
            } catch (RuntimeException e) {
                // retried on the next run
                log.error("Could not close the rating window of match {}", matchId, e);
            }
        }
    }
}
