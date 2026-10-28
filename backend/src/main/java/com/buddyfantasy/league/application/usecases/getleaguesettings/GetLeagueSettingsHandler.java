package com.buddyfantasy.league.application.usecases.getleaguesettings;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.UnexpectedException;
import org.springframework.stereotype.Component;

@Component
class GetLeagueSettingsHandler implements QueryHandler<GetLeagueSettingsQuery, LeagueSettingsView> {

    private final LeagueAccess access;
    private final LeagueRepository leagues;

    GetLeagueSettingsHandler(LeagueAccess access, LeagueRepository leagues) {
        this.access = access;
        this.leagues = leagues;
    }

    @Override
    public LeagueSettingsView handle(GetLeagueSettingsQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        League league = leagues.findById(query.leagueId())
                .orElseThrow(() -> new UnexpectedException("League not found: " + query.leagueId()));
        return new LeagueSettingsView(league.settings(), league.scoringRules());
    }
}
