package com.buddyfantasy.league.application.usecases.regenerateinvitation;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** Revokes the active invitation (if any) and issues a new one; returns the raw token, shown only once. */
public record RegenerateInvitationCommand(UUID userId, UUID leagueId) implements Command<String> {
}
