package com.buddyfantasy.match.adapter.in.web;

import java.time.Instant;

import com.buddyfantasy.match.domain.Venue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code kickoffAt} is an ISO-8601 instant in UTC. The domain checks the remaining rules, including
 * {@code match.limits}.
 */
record MatchRequest(@NotNull Integer matchday,
                    @NotBlank String opponent,
                    @NotNull Venue venue,
                    @NotNull Instant kickoffAt) {
}
