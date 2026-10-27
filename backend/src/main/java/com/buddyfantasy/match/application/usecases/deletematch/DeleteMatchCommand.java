package com.buddyfantasy.match.application.usecases.deletematch;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * The league admin removes a scheduled match that will not be replayed (suspended). Physical delete; its audit
 * history is kept. A closed match is never deleted.
 */
public record DeleteMatchCommand(UUID userId, UUID leagueId, UUID matchId) implements Command<Void> {
}
