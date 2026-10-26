package com.buddyfantasy.league.application.usecases.getleague;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.shared.application.QueryHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class GetLeagueHandler implements QueryHandler<GetLeagueQuery, LeagueView> {

    private final LeagueAccess access;
    private final LeagueRepository leagues;

    GetLeagueHandler(LeagueAccess access, LeagueRepository leagues) {
        this.access = access;
        this.leagues = leagues;
    }

    @Override
    public LeagueView handle(GetLeagueQuery query) {
        LeagueRole role = access.roleOf(query.userId(), query.leagueId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of league " + query.leagueId()));
        League league = leagues.findById(query.leagueId()).orElseThrow();
        return new LeagueView(league.id().value(), league.name(), league.createdAt(), role);
    }
}
