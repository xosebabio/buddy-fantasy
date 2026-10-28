package com.buddyfantasy.league.domain;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.shared.domain.ConflictException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/** A user's single role in a league. {@code userId} is a logical reference to the identity module. */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class Membership {

    private final UUID id;
    private final LeagueId leagueId;
    private final UUID userId;
    private final LeagueRole role;
    private final Instant joinedAt;

    public static Membership join(LeagueId leagueId, UUID userId, LeagueRole role, Instant now) {
        return new Membership(UUID.randomUUID(), leagueId, userId, role, now);
    }

    /** The league always keeps its admin: they can neither be removed nor leave (changing admin is out of v1). */
    public void requireRemovable() {
        if (role == LeagueRole.ADMIN) {
            throw new ConflictException("The league admin cannot be removed or leave the league");
        }
    }
}
