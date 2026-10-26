package com.buddyfantasy.league.domain;

import java.util.UUID;

public record LeagueId(UUID value) {

    public static LeagueId generate() {
        return new LeagueId(UUID.randomUUID());
    }
}
