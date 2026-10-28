package com.buddyfantasy.scoring.application.usecases.applylockedlineups;

import java.time.Clock;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.shared.application.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The matchday and kickoff of the lock only stand until the sheet arrives with the final ones. The league's table is
 * only read if the match is scored for the first time.
 */
@Component
@RequiredArgsConstructor
class ApplyLockedLineupsHandler implements CommandHandler<ApplyLockedLineupsCommand, Void> {

    private final MatchScoreRepository scores;
    private final LeagueSettingsProvider leagueSettings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(ApplyLockedLineupsCommand command) {
        MatchScore score = scores.lockOrCreate(MatchScore.create(command.matchId(), command.leagueId(),
                command.matchday(), command.kickoffAt()));
        if (!score.applyLineups(command.lineups(), command.occurredAt())) {
            return null;
        }
        score.score(() -> leagueSettings.scoringRules(command.leagueId()), clock.instant());
        scores.save(score);
        score.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
