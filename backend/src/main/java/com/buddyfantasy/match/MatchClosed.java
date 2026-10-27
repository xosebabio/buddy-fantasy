package com.buddyfantasy.match;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * The admin closed a match. Carries the full sheet so {@code scoring} and {@code rating} keep their own copy;
 * applying it twice gives the same result. {@code rating} opens the rating window from here (RF-17).
 */
public record MatchClosed(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<PlayerPerformanceData> performances, Instant occurredAt) implements DomainEvent {

    public MatchClosed {
        performances = List.copyOf(performances);
    }
}
