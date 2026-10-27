package com.buddyfantasy.match.application.usecases.listmatches;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;

/** A calendar row. {@code result} is null until recorded, and for managers until the match is closed. */
public record MatchSummaryView(UUID id, int matchday, String opponent, Venue venue, Instant kickoffAt,
                               MatchStatus status, Instant closedAt, MatchResult result) {

    public MatchSummaryView withoutResult() {
        return new MatchSummaryView(id, matchday, opponent, venue, kickoffAt, status, closedAt, null);
    }
}
