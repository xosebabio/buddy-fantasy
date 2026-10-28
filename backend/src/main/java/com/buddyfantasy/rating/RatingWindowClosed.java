package com.buddyfantasy.rating;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * The rating window of a match closed (RF-17): the result of every player in its sheet, rated or not (RF-22, RF-23).
 * Published once per window.
 */
public record RatingWindowClosed(UUID leagueId, UUID matchId, List<PlayerRatingData> players, Instant occurredAt)
        implements DomainEvent {

    public RatingWindowClosed {
        players = List.copyOf(players);
    }
}
