package com.buddyfantasy.match.domain;

import java.util.UUID;

public record MatchId(UUID value) {

    public static MatchId generate() {
        return new MatchId(UUID.randomUUID());
    }
}
