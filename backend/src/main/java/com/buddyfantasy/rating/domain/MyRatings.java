package com.buddyfantasy.rating.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * A member's votes in a match (RF-18 to RF-20): optional and per player, anonymous, and editable while the window is
 * open. Only their author ever sees them (RF-21).
 */
public record MyRatings(UUID matchId, UUID userId, List<Vote> votes) {

    public MyRatings {
        votes = List.copyOf(votes);
    }

    public static MyRatings none(UUID matchId, UUID userId) {
        return new MyRatings(matchId, userId, List.of());
    }

    /**
     * Replaces all the votes with {@code newVotes}: a player missing from them loses the member's vote. The window
     * must be open at {@code now} ({@link RatingError#RATING_WINDOW_CLOSED}) and the member not {@code excluded} by
     * the admin ({@link RatingError#RATER_EXCLUDED}), both 409; each player once
     * ({@link RatingError#DUPLICATE_PLAYER}), in the sheet ({@link RatingError#PLAYER_NOT_IN_SHEET}) and rated in the window's format
     * ({@link RatingError#INVALID_RATING}), all 400.
     */
    public MyRatings replace(List<Vote> newVotes, RatingWindow window, boolean excluded, Instant now) {
        if (!window.matchId().equals(matchId)) {
            throw new DomainValidationException("The votes belong to match " + matchId);
        }
        window.requireOpen(now);
        if (excluded) {
            throw new ConflictException(RatingError.RATER_EXCLUDED,
                    "The admin cancelled your ratings of match " + matchId);
        }
        if (newVotes.stream().map(Vote::playerId).distinct().count() != newVotes.size()) {
            throw new DomainValidationException(RatingError.DUPLICATE_PLAYER, "A player is rated more than once");
        }
        newVotes.forEach(vote -> {
            if (!window.hasPlayer(vote.playerId())) {
                throw new DomainValidationException(RatingError.PLAYER_NOT_IN_SHEET,
                        "Player " + vote.playerId() + " did not play match " + matchId);
            }
            window.rules().requireValidVote(vote.value());
        });
        return new MyRatings(matchId, userId, newVotes);
    }
}
