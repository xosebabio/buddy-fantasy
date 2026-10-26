package com.buddyfantasy.squad.domain;

import java.util.UUID;

public record PlayerId(UUID value) {

    public static PlayerId generate() {
        return new PlayerId(UUID.randomUUID());
    }
}
