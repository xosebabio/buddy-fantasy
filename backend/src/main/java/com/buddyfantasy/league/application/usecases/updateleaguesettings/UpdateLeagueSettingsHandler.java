package com.buddyfantasy.league.application.usecases.updateleaguesettings;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class UpdateLeagueSettingsHandler implements CommandHandler<UpdateLeagueSettingsCommand, Void> {

    private final LeagueAccess access;
    private final LeagueRepository leagues;
    private final LeagueLimitsRepository limits;

    UpdateLeagueSettingsHandler(LeagueAccess access, LeagueRepository leagues, LeagueLimitsRepository limits) {
        this.access = access;
        this.leagues = leagues;
        this.limits = limits;
    }

    @Override
    public Void handle(UpdateLeagueSettingsCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        League league = leagues.findById(command.leagueId()).orElseThrow();
        leagues.save(league.withSettings(command.settings(), command.scoringRules(), limits.get()));
        return null;
    }
}
