package com.buddyfantasy.league.domain;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A user's single role in a league. {@code userId} is a logical reference to the identity module. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class Membership {

    private final UUID id;
    private final LeagueId leagueId;
    private final UUID userId;
    private final LeagueRole role;
    private final Instant joinedAt;

    public static Membership join(LeagueId leagueId, UUID userId, LeagueRole role, Instant now) {
        return new Membership(UUID.randomUUID(), leagueId, userId, role, now);
    }
}
