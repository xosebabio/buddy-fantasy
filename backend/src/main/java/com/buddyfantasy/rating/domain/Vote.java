package com.buddyfantasy.rating.domain;

import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainValidationException;

/** A member's rating of a player in a match (RF-19): a whole value in the window's format. */
public record Vote(UUID playerId, int value) {

    public Vote {
        if (playerId == null) {
            throw new DomainValidationException("Player is required");
        }
    }
}
