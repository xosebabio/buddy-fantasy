package com.buddyfantasy.scoring.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.application.usecases.getmanagerhistory.GetManagerHistoryQuery;
import com.buddyfantasy.scoring.application.usecases.getmanagerhistory.ManagerMatchdayView;
import com.buddyfantasy.scoring.application.usecases.getplayerpoints.GetPlayerPointsQuery;
import com.buddyfantasy.scoring.application.usecases.getplayerpoints.PlayerMatchdayView;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The points history of a manager (RF-33) and of a player (RF-11), for every member. Read only. */
@RestController
class ScoringHistoryController {

    private final CommandBus bus;

    ScoringHistoryController(CommandBus bus) {
        this.bus = bus;
    }

    @GetMapping("/api/leagues/{id}/managers/{userId}/history")
    List<ManagerMatchdayView> manager(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                      @PathVariable("userId") UUID managerId) {
        return bus.ask(new GetManagerHistoryQuery(user.userId(), id, managerId));
    }

    @GetMapping("/api/leagues/{id}/players/{playerId}/points")
    List<PlayerMatchdayView> player(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                    @PathVariable UUID playerId) {
        return bus.ask(new GetPlayerPointsQuery(user.userId(), id, playerId));
    }
}
