package com.buddyfantasy.lineup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * The lineups of a match were locked (RF-25, RF-27). Carries every member's locked lineup so {@code scoring} keeps
 * its own copy and computes the managers' points (RF-31) once it has both this and {@code MatchClosed}, in any
 * order. Members without a lineup saved before the lock are absent.
 */
public record LineupsLocked(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<LockedLineupData> lineups, Instant occurredAt) implements DomainEvent {

    public LineupsLocked {
        lineups = List.copyOf(lineups);
    }
}
