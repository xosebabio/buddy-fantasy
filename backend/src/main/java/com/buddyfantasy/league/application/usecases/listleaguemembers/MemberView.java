package com.buddyfantasy.league.application.usecases.listleaguemembers;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;

/** A league member; the admin is listed too, as they play as manager. */
public record MemberView(UUID userId, String displayName, String avatarUrl, LeagueRole role, Instant joinedAt) {
}
