package com.buddyfantasy.scoring.application.usecases.applymatchsheet;

import java.time.Clock;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The league's table is only read if the match is scored for the first time; a correction reuses the frozen one and
 * publishes {@code MatchdayScored} again only if it changes some points.
 */
@Component
class ApplyMatchSheetHandler implements CommandHandler<ApplyMatchSheetCommand, Void> {

    private final MatchScoreRepository scores;
    private final LeagueSettingsProvider leagueSettings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ApplyMatchSheetHandler(MatchScoreRepository scores, LeagueSettingsProvider leagueSettings,
                           ApplicationEventPublisher events, Clock clock) {
        this.scores = scores;
        this.leagueSettings = leagueSettings;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Void handle(ApplyMatchSheetCommand command) {
        MatchScore score = scores.lockOrCreate(MatchScore.create(command.matchId(), command.leagueId(),
                command.matchday(), command.kickoffAt()));
        if (!score.applySheet(command.matchday(), command.kickoffAt(), command.performances(),
                command.occurredAt())) {
            return null;
        }
        score.score(() -> leagueSettings.scoringRules(command.leagueId()), clock.instant());
        scores.save(score);
        score.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
