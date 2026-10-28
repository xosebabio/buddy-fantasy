package com.buddyfantasy.scoring.application.usecases.applyratings;

import java.time.Clock;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.UnexpectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The ratings' events do not bring the matchday and kickoff: if scoring has not heard of the match yet, because its
 * {@code MatchClosed} failed and will be delivered again, it is created from the calendar, where a closed match always
 * is. The league's table is only read if the match is scored for the first time.
 */
@Component
@RequiredArgsConstructor
class ApplyRatingsHandler implements CommandHandler<ApplyRatingsCommand, Void> {

    private final MatchScoreRepository scores;
    private final MatchApi matches;
    private final LeagueSettingsProvider leagueSettings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(ApplyRatingsCommand command) {
        MatchScore score = scores.lock(command.matchId()).orElseGet(() -> create(command));
        if (!score.applyRatings(command.ratingPoints(), command.occurredAt())) {
            return null;
        }
        score.score(() -> leagueSettings.scoringRules(command.leagueId()), clock.instant());
        scores.save(score);
        score.pullEvents().forEach(events::publishEvent);
        return null;
    }

    private MatchScore create(ApplyRatingsCommand command) {
        return matches.findMatches(command.leagueId()).stream()
                .filter(match -> match.id().equals(command.matchId()))
                .findFirst()
                .map(match -> scores.lockOrCreate(MatchScore.create(match.id(), match.leagueId(), match.matchday(),
                        match.kickoffAt())))
                .orElseThrow(() -> new UnexpectedException("Rated match not in the calendar: " + command.matchId()));
    }
}
