package com.buddyfantasy.lineup.adapter.in.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The whole lineup, in any order. The domain checks size, players, formation, budget and changes. */
record LineupRequest(@NotNull List<@NotNull UUID> playerIds) {
}
