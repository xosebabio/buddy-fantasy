package com.buddyfantasy.match.application.usecases.closematch;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * The league admin closes a match once it has started and its sheet has a result and at least one player. Publishes
 * {@link com.buddyfantasy.match.MatchClosed}, which opens the rating window (RF-17). Idempotent.
 */
public record CloseMatchCommand(UUID userId, UUID leagueId, UUID matchId) implements Command<Void> {
}
