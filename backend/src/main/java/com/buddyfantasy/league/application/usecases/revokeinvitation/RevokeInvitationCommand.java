package com.buddyfantasy.league.application.usecases.revokeinvitation;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** Disables the invitation link without creating a new one. Idempotent. */
public record RevokeInvitationCommand(UUID userId, UUID leagueId) implements Command<Void> {
}
