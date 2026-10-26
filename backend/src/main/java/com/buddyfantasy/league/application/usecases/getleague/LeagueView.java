package com.buddyfantasy.league.application.usecases.getleague;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;

/** A league as seen by one of its members, with the member's own role. */
public record LeagueView(UUID id, String name, Instant createdAt, LeagueRole role) {
}
