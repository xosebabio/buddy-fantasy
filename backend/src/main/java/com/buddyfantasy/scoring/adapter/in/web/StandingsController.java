package com.buddyfantasy.scoring.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.application.usecases.getmatchdayscores.GetMatchdayScoresQuery;
import com.buddyfantasy.scoring.application.usecases.getmatchdayscores.MatchdayScoresView;
import com.buddyfantasy.scoring.application.usecases.getstandings.GetStandingsQuery;
import com.buddyfantasy.scoring.application.usecases.getstandings.StandingView;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The overall standings of a league and those of each scored match (RF-32), for every member. Read only. */
@RestController
@RequiredArgsConstructor
class StandingsController {

    private final CommandBus bus;

    @GetMapping("/api/leagues/{id}/standings")
    List<StandingView> overall(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new GetStandingsQuery(user.userId(), id));
    }

    @GetMapping("/api/leagues/{id}/standings/{matchId}")
    MatchdayScoresView matchday(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                @PathVariable UUID matchId) {
        return bus.ask(new GetMatchdayScoresQuery(user.userId(), id, matchId));
    }
}
