package com.buddyfantasy.lineup.application.usecases.lockmatchlineups;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * RF-25, RF-27: locks the lineups of a match whose lock time has come, from the lock job or when the match is
 * closed. Idempotent: returns true only for the call that locked it and published
 * {@link com.buddyfantasy.lineup.LineupsLocked}; false if it was already locked or the match no longer exists.
 */
public record LockMatchLineupsCommand(UUID leagueId, UUID matchId) implements Command<Boolean> {
}
