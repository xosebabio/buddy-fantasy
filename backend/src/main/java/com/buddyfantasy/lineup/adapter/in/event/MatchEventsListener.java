package com.buddyfantasy.lineup.adapter.in.event;

import com.buddyfantasy.lineup.application.usecases.deletematchlock.DeleteMatchLockCommand;
import com.buddyfantasy.lineup.application.usecases.lockmatchlineups.LockMatchLineupsCommand;
import com.buddyfantasy.lineup.application.usecases.reschedulematchlock.RescheduleMatchLockCommand;
import com.buddyfantasy.match.MatchClosed;
import com.buddyfantasy.match.MatchDeleted;
import com.buddyfantasy.match.MatchRescheduled;
import com.buddyfantasy.shared.application.CommandBus;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Keeps the lineup locks in step with the calendar. Delivery is at least once and every command is idempotent. */
@Component
@RequiredArgsConstructor
class MatchEventsListener {

    private final CommandBus bus;

    /** Safety net: locks a match the job did not reach, or one loaded into the calendar already played. */
    @ApplicationModuleListener
    void on(MatchClosed event) {
        bus.dispatch(new LockMatchLineupsCommand(event.leagueId(), event.matchId()));
    }

    @ApplicationModuleListener
    void on(MatchRescheduled event) {
        bus.dispatch(new RescheduleMatchLockCommand(event.matchId(), event.kickoffAt()));
    }

    @ApplicationModuleListener
    void on(MatchDeleted event) {
        bus.dispatch(new DeleteMatchLockCommand(event.matchId()));
    }
}
