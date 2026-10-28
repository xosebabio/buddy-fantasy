package com.buddyfantasy.rating.adapter.in.event;

import java.util.List;

import com.buddyfantasy.match.MatchClosed;
import com.buddyfantasy.match.MatchCorrected;
import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.rating.application.usecases.applycorrectedsheet.ApplyCorrectedSheetCommand;
import com.buddyfantasy.rating.application.usecases.openratingwindow.OpenRatingWindowCommand;
import com.buddyfantasy.rating.domain.SheetPlayer;
import com.buddyfantasy.shared.application.CommandBus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Opens the rating window of a closed match and keeps rating's copy of its sheet ({@code lineup} and {@code scoring}
 * already have beans named after their match listeners). Delivery is at least once and every command is idempotent.
 */
@Component
class ClosedMatchEventsListener {

    private final CommandBus bus;

    ClosedMatchEventsListener(CommandBus bus) {
        this.bus = bus;
    }

    /** RF-17. */
    @ApplicationModuleListener
    void on(MatchClosed event) {
        bus.dispatch(new OpenRatingWindowCommand(event.leagueId(), event.matchId(), event.matchday(),
                event.kickoffAt(), toSheet(event.performances()), event.occurredAt()));
    }

    /** RF-16: the players removed from the sheet lose their votes. */
    @ApplicationModuleListener
    void on(MatchCorrected event) {
        bus.dispatch(new ApplyCorrectedSheetCommand(event.leagueId(), event.matchId(), event.matchday(),
                event.kickoffAt(), toSheet(event.performances()), event.occurredAt()));
    }

    private static List<SheetPlayer> toSheet(List<PlayerPerformanceData> performances) {
        return performances.stream().map(line -> new SheetPlayer(line.playerId(), line.position())).toList();
    }
}
