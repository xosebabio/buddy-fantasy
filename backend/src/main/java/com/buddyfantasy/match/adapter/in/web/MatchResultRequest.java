package com.buddyfantasy.match.adapter.in.web;

import jakarta.validation.constraints.NotNull;

record MatchResultRequest(@NotNull Integer goalsFor, @NotNull Integer goalsAgainst) {
}
