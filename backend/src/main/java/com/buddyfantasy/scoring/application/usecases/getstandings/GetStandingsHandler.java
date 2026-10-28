package com.buddyfantasy.scoring.application.usecases.getstandings;

import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.Standings;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** One query for the managers' points of every scored match and one for the members. */
@Component
@RequiredArgsConstructor
class GetStandingsHandler implements QueryHandler<GetStandingsQuery, List<StandingView>> {

    private final LeagueAccess access;
    private final MatchScoreRepository scores;

    @Override
    public List<StandingView> handle(GetStandingsQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        return Standings.overall(scores.findScoredMatchdayPoints(query.leagueId()),
                        access.memberSince(query.leagueId())).stream()
                .map(StandingView::from)
                .toList();
    }
}
