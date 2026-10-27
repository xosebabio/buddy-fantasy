package com.buddyfantasy.match.application.usecases.listmatches;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/** RF-12: the league's calendar by matchday, for any member. */
public record ListMatchesQuery(UUID userId, UUID leagueId) implements Query<List<MatchSummaryView>> {
}
