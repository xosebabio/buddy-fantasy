package com.buddyfantasy.league.application.usecases.updateleaguesettings;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.UnexpectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UpdateLeagueSettingsHandler implements CommandHandler<UpdateLeagueSettingsCommand, Void> {

    private final LeagueAccess access;
    private final LeagueRepository leagues;
    private final LeagueLimitsRepository limits;

    @Override
    public Void handle(UpdateLeagueSettingsCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        League league = leagues.findById(command.leagueId())
                .orElseThrow(() -> new UnexpectedException("League not found: " + command.leagueId()));
        leagues.save(league.withSettings(command.settings(), command.scoringRules(), limits.get()));
        return null;
    }
}
