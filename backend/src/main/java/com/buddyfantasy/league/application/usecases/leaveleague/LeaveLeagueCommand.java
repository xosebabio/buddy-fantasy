package com.buddyfantasy.league.application.usecases.leaveleague;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** A manager leaves the league. The admin cannot leave. */
public record LeaveLeagueCommand(UUID userId, UUID leagueId) implements Command<Void> {
}
