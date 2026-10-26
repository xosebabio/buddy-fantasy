package com.buddyfantasy.league.application.usecases.createleague;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** RF-05: creates a league with default settings; {@code creatorId} becomes its ADMIN. */
public record CreateLeagueCommand(UUID creatorId, String name) implements Command<CreatedLeague> {
}
