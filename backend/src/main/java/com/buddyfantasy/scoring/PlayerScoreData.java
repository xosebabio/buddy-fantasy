package com.buddyfantasy.scoring;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;

/**
 * A player's points in a match as published in {@link MatchdayScored}: {@code points = actionPoints + ratingPoints},
 * without weights. {@code position} is the one in the sheet.
 */
public record PlayerScoreData(UUID playerId, Position position, int actionPoints, int ratingPoints, int points) {
}
