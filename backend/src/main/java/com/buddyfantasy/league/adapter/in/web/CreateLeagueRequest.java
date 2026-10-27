package com.buddyfantasy.league.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/** The domain checks the length against {@code league.limits.name-max-length}. */
record CreateLeagueRequest(@NotBlank String name) {
}
