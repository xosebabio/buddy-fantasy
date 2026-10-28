package com.buddyfantasy.scoring;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * A match was scored (RF-15, RF-30, RF-31): the points of every player in its sheet, lined up or not, and of every
 * manager with a locked lineup. Published the first time the match is scored and again only if a recalculation
 * (RF-16) changes it: a consumer replaces whatever it had of that match. Members without a lineup are absent and
 * score 0.
 */
public record MatchdayScored(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<PlayerScoreData> players, List<ManagerScoreData> managers, Instant occurredAt) implements DomainEvent {

    public MatchdayScored {
        players = List.copyOf(players);
        managers = List.copyOf(managers);
    }
}
