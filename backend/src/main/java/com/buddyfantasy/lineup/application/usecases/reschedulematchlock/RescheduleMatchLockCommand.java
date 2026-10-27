package com.buddyfantasy.lineup.application.usecases.reschedulematchlock;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * A match was postponed to {@code kickoffAt}. If it was locked and its new lock time is in the future, its lineups
 * are unlocked ({@link com.buddyfantasy.lineup.LineupsUnlocked}) and locked again when the new lock time comes, with
 * the versions saved before it. Otherwise nothing changes. Idempotent.
 */
public record RescheduleMatchLockCommand(UUID matchId, Instant kickoffAt) implements Command<Void> {
}
