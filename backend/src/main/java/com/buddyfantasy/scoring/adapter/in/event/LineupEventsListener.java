package com.buddyfantasy.scoring.adapter.in.event;

import com.buddyfantasy.lineup.LineupsLocked;
import com.buddyfantasy.lineup.LineupsUnlocked;
import com.buddyfantasy.scoring.application.usecases.applylockedlineups.ApplyLockedLineupsCommand;
import com.buddyfantasy.scoring.application.usecases.discardlockedlineups.DiscardLockedLineupsCommand;
import com.buddyfantasy.scoring.application.usecases.discardlockedlineups.DiscardLockedLineupsCommand.Cause;
import com.buddyfantasy.scoring.domain.ManagerLineup;
import com.buddyfantasy.shared.application.CommandBus;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Keeps scoring's copy of the locked lineups. Delivery is at least once and every command is idempotent. */
@Component
@RequiredArgsConstructor
class LineupEventsListener {

    private final CommandBus bus;

    @ApplicationModuleListener
    void on(LineupsLocked event) {
        bus.dispatch(new ApplyLockedLineupsCommand(event.leagueId(), event.matchId(), event.matchday(),
                event.kickoffAt(), event.lineups().stream()
                .map(lineup -> new ManagerLineup(lineup.userId(), lineup.playerIds(), lineup.overBudget()))
                .toList(), event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(LineupsUnlocked event) {
        bus.dispatch(new DiscardLockedLineupsCommand(event.leagueId(), event.matchId(), Cause.UNLOCKED,
                event.occurredAt()));
    }
}
