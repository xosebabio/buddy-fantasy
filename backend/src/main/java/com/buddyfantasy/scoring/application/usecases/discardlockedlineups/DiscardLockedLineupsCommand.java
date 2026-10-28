package com.buddyfantasy.scoring.application.usecases.discardlockedlineups;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * The locked lineups of a match no longer stand, because it was postponed and unlocked ({@link Cause#UNLOCKED}) or
 * deleted ({@link Cause#MATCH_DELETED}). Both only happen before the match is closed, so they never drop points.
 * Idempotent: {@code occurredAt} is kept to the microsecond, as stored, so the same event delivered again is never
 * newer.
 */
public record DiscardLockedLineupsCommand(UUID leagueId, UUID matchId, Cause cause, Instant occurredAt)
        implements Command<Void> {

    public DiscardLockedLineupsCommand {
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }

    public enum Cause {
        /** Drops the copy of the lineups locked before {@code occurredAt} and ignores those delivered late. */
        UNLOCKED,
        /** Drops scoring's match, unless it has a sheet. */
        MATCH_DELETED
    }
}
