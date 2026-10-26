package com.buddyfantasy.squad.application.usecases.retireplayer;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** Soft delete by the league admin: the player leaves the active squad and frees their shirt number. Idempotent. */
public record RetirePlayerCommand(UUID userId, UUID leagueId, UUID playerId) implements Command<Void> {
}
