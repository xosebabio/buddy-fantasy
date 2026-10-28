package com.buddyfantasy.scoring.application.usecases.getstandings;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * The overall standings of the league (RF-32): its current members, each with the points of the scored matches since
 * they joined. A member who has not scored yet is there with 0.
 */
public record GetStandingsQuery(UUID userId, UUID leagueId) implements Query<List<StandingView>> {
}
