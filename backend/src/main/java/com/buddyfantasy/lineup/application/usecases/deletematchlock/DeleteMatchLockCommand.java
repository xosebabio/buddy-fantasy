package com.buddyfantasy.lineup.application.usecases.deletematchlock;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** A match was deleted: its lock and locked lineups go with it, without any event. Idempotent. */
public record DeleteMatchLockCommand(UUID matchId) implements Command<Void> {
}
