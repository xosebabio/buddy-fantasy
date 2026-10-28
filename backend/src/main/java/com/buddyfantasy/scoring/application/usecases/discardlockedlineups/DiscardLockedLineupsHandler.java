package com.buddyfantasy.scoring.application.usecases.discardlockedlineups;

import java.util.Optional;

import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * An unlock is remembered even if scoring has not heard of the match yet, because its {@code LineupsLocked} failed
 * and will be delivered again: then the match is created from the calendar, so that late lock is ignored. A
 * {@code LineupsLocked} delivered late after a deletion recreates a match without a sheet, which is never scored.
 */
@Component
class DiscardLockedLineupsHandler implements CommandHandler<DiscardLockedLineupsCommand, Void> {

    private final MatchScoreRepository scores;
    private final MatchApi matches;

    DiscardLockedLineupsHandler(MatchScoreRepository scores, MatchApi matches) {
        this.scores = scores;
        this.matches = matches;
    }

    @Override
    public Void handle(DiscardLockedLineupsCommand command) {
        switch (command.cause()) {
            case UNLOCKED -> unlock(command);
            case MATCH_DELETED -> scores.lock(command.matchId())
                    .filter(score -> !score.hasSheet())
                    .ifPresent(score -> scores.delete(score.matchId()));
        }
        return null;
    }

    private void unlock(DiscardLockedLineupsCommand command) {
        Optional<MatchScore> stored = scores.lock(command.matchId()).or(() -> create(command));
        stored.filter(score -> score.unlock(command.occurredAt())).ifPresent(scores::save);
    }

    /** The match as the calendar has it now; empty if it was deleted since. */
    private Optional<MatchScore> create(DiscardLockedLineupsCommand command) {
        return matches.findMatches(command.leagueId()).stream()
                .filter(match -> match.id().equals(command.matchId()))
                .findFirst()
                .map(match -> scores.lockOrCreate(MatchScore.create(match.id(), match.leagueId(),
                        match.matchday(), match.kickoffAt())));
    }
}
