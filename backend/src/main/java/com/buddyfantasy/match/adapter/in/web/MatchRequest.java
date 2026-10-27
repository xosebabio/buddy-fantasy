package com.buddyfantasy.match.adapter.in.web;

import java.time.Instant;

import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.Venue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code kickoffAt} is an ISO-8601 instant in UTC. The domain checks the remaining rules. */
record MatchRequest(@NotNull Integer matchday,
                    @NotBlank @Size(max = Match.MAX_OPPONENT_LENGTH) String opponent,
                    @NotNull Venue venue,
                    @NotNull Instant kickoffAt) {
}
