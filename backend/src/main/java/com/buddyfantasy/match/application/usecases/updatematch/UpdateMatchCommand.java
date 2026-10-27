package com.buddyfantasy.match.application.usecases.updatematch;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.Command;

/**
 * Full replacement of a match's details by the league admin; changing matchday or kickoff is a postponement. On a
 * closed match only opponent and venue may change.
 */
public record UpdateMatchCommand(UUID userId, UUID leagueId, UUID matchId, int matchday, String opponent,
                                 Venue venue, Instant kickoffAt) implements Command<Void> {
}
