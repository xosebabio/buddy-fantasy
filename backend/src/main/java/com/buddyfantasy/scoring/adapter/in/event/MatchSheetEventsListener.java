package com.buddyfantasy.scoring.adapter.in.event;

import com.buddyfantasy.match.MatchClosed;
import com.buddyfantasy.match.MatchCorrected;
import com.buddyfantasy.match.MatchDeleted;
import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.scoring.application.usecases.applymatchsheet.ApplyMatchSheetCommand;
import com.buddyfantasy.scoring.application.usecases.discardlockedlineups.DiscardLockedLineupsCommand;
import com.buddyfantasy.scoring.application.usecases.discardlockedlineups.DiscardLockedLineupsCommand.Cause;
import com.buddyfantasy.scoring.domain.PlayerPerformance;
import com.buddyfantasy.shared.application.CommandBus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Keeps scoring's copy of the sheets ({@code lineup} already has a {@code MatchEventsListener} bean). Delivery is
 * at least once and every command is idempotent.
 */
@Component
class MatchSheetEventsListener {

    private final CommandBus bus;

    MatchSheetEventsListener(CommandBus bus) {
        this.bus = bus;
    }

    @ApplicationModuleListener
    void on(MatchClosed event) {
        bus.dispatch(new ApplyMatchSheetCommand(event.leagueId(), event.matchId(), event.matchday(),
                event.kickoffAt(), event.performances().stream().map(MatchSheetEventsListener::toPerformance).toList(),
                event.occurredAt()));
    }

    /** RF-16: replaces the copy of the sheet and recalculates the match with its frozen table. */
    @ApplicationModuleListener
    void on(MatchCorrected event) {
        bus.dispatch(new ApplyMatchSheetCommand(event.leagueId(), event.matchId(), event.matchday(),
                event.kickoffAt(), event.performances().stream().map(MatchSheetEventsListener::toPerformance).toList(),
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(MatchDeleted event) {
        bus.dispatch(new DiscardLockedLineupsCommand(event.leagueId(), event.matchId(), Cause.MATCH_DELETED,
                event.occurredAt()));
    }

    private static PlayerPerformance toPerformance(PlayerPerformanceData line) {
        return new PlayerPerformance(line.playerId(), line.position(), line.goals(), line.penaltyGoals(),
                line.penaltiesSaved(), line.penaltiesMissed(), line.assists(), line.ownGoals(), line.yellowCard(),
                line.doubleYellow(), line.redCard());
    }
}
