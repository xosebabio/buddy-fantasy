package com.buddyfantasy.scoring.application.usecases.applyratings;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * RF-22, RF-23, RF-30: the rating window of a match closed ({@code RatingWindowClosed}) or a correction changed its
 * result ({@code RatingsCorrected}) with these points for ratings by player. scoring keeps its copy, unless it already
 * has one from a more recent event, and scores the match if it also has its sheet and locked lineups; after a
 * correction it recalculates it with the table frozen when it was first scored. Idempotent: applying it again changes
 * nothing.
 * {@code occurredAt} is kept to the microsecond, as stored, so the same event delivered again is never newer.
 */
public record ApplyRatingsCommand(UUID leagueId, UUID matchId, Map<UUID, Integer> ratingPoints, Instant occurredAt)
        implements Command<Void> {

    public ApplyRatingsCommand {
        ratingPoints = Map.copyOf(ratingPoints);
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }
}
