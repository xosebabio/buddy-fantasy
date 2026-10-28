package com.buddyfantasy.rating.adapter.in.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * All the member's votes in the match, in any order; the players missing lose the member's vote. The domain checks
 * the window, the players and the format.
 */
record RatingsRequest(@NotNull List<@NotNull @Valid Rating> ratings) {

    /** A whole value: a decimal one is rejected when reading the body. */
    record Rating(@NotNull UUID playerId, @NotNull Integer value) {
    }
}
