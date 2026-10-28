package com.buddyfantasy.rating;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * A correction of the sheet (RF-16) changed the ratings of a match whose window was already closed: a player was
 * added (unrated) or removed (with his votes). Same shape as {@link RatingWindowClosed}: a consumer replaces whatever
 * it had of that match.
 */
public record RatingsCorrected(UUID leagueId, UUID matchId, List<PlayerRatingData> players, Instant occurredAt)
        implements DomainEvent {

    public RatingsCorrected {
        players = List.copyOf(players);
    }
}
