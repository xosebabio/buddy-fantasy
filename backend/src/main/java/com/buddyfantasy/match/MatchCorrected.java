package com.buddyfantasy.match;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * The admin replaced the sheet of a closed match (RF-16). Carries the full new sheet, with the matchday and kickoff
 * as in {@link MatchClosed} (a closed match never changes them): {@code scoring} recalculates the matchday and
 * {@code rating} drops the ratings of players no longer in it.
 */
public record MatchCorrected(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<PlayerPerformanceData> performances, Instant occurredAt) implements DomainEvent {

    public MatchCorrected {
        performances = List.copyOf(performances);
    }
}
