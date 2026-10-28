package com.buddyfantasy.scoring.application.usecases.getplayerpoints;

import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.PlayerMatchday;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.SquadApi;
import org.springframework.stereotype.Component;

/**
 * Reads scoring's copies of the sheets, which only hold players of the league. Only when the player has no points
 * does it look at the squad, to tell a player of another league or unknown (404) from one who has not played yet.
 */
@Component
class GetPlayerPointsHandler implements QueryHandler<GetPlayerPointsQuery, List<PlayerMatchdayView>> {

    private final LeagueAccess access;
    private final MatchScoreRepository scores;
    private final SquadApi squad;

    GetPlayerPointsHandler(LeagueAccess access, MatchScoreRepository scores, SquadApi squad) {
        this.access = access;
        this.scores = scores;
        this.squad = squad;
    }

    @Override
    public List<PlayerMatchdayView> handle(GetPlayerPointsQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        List<PlayerMatchday> matchdays = scores.findPlayerMatchdays(query.leagueId(), query.playerId());
        if (matchdays.isEmpty() && squad.findPlayers(query.leagueId(), List.of(query.playerId())).isEmpty()) {
            throw new NotFoundException("Player " + query.playerId() + " not in league " + query.leagueId());
        }
        return matchdays.stream().map(PlayerMatchdayView::from).toList();
    }
}
