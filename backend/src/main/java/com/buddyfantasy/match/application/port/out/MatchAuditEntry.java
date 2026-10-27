package com.buddyfantasy.match.application.port.out;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.domain.MatchAuditAction;

/** One change of the admin to a match (RNF-04). {@code before} is null on scheduling and {@code after} on deletion. */
public record MatchAuditEntry(UUID leagueId, UUID matchId, UUID actorUserId, MatchAuditAction action,
                              Instant occurredAt, MatchSnapshot before, MatchSnapshot after) {
}
