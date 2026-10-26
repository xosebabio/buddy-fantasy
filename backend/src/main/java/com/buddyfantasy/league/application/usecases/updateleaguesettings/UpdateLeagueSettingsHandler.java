package com.buddyfantasy.league.application.usecases.updateleaguesettings;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class UpdateLeagueSettingsHandler implements CommandHandler<UpdateLeagueSettingsCommand, Void> {

    private final LeagueAccess access;
    private final LeagueRepository leagues;

    UpdateLeagueSettingsHandler(LeagueAccess access, LeagueRepository leagues) {
        this.access = access;
        this.leagues = leagues;
    }

    @Override
    public Void handle(UpdateLeagueSettingsCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        League league = leagues.findById(command.leagueId()).orElseThrow();
        leagues.save(league.withSettings(command.settings(), command.scoringRules()));
        return null;
    }
}
