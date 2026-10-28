package com.buddyfantasy.match.application.usecases.getmatch;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.match.application.port.out.MatchRepository;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.ForbiddenException;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class GetMatchHandler implements QueryHandler<GetMatchQuery, MatchView> {

    private final LeagueAccess access;
    private final MatchRepository matches;

    @Override
    public MatchView handle(GetMatchQuery query) {
        LeagueRole role = access.roleOf(query.userId(), query.leagueId())
                .orElseThrow(() -> new ForbiddenException("Not a member of league " + query.leagueId()));
        return matches.findByLeagueIdAndId(query.leagueId(), new MatchId(query.matchId()))
                .map(match -> MatchView.from(match, role == LeagueRole.ADMIN))
                .orElseThrow(() -> new NotFoundException("Match " + query.matchId() + " not in league " + query.leagueId()));
    }
}
