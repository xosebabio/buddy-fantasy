package com.buddyfantasy.rating.application.usecases.getmatchratings;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.rating.domain.PlayerRatingResult;
import com.buddyfantasy.rating.domain.SheetPlayer;

/**
 * A player of the sheet with the position he played in and the member's own vote ({@code myRating}, null if not
 * rated). {@code votes}, {@code average} and {@code ratingPoints} are null until the window is closed, and
 * {@code average} also without votes. Never another member's vote (RF-21).
 */
public record PlayerRatingView(UUID playerId, Position position, Integer myRating, Integer votes,
                               BigDecimal average, Integer ratingPoints) {

    /** {@code result} is null while the window is not closed. */
    static PlayerRatingView of(SheetPlayer player, Integer myRating, PlayerRatingResult result) {
        return new PlayerRatingView(player.playerId(), player.position(), myRating,
                result == null ? null : result.votes(),
                result == null ? null : result.average(),
                result == null ? null : result.ratingPoints());
    }
}
