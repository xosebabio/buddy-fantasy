package com.buddyfantasy.rating.domain;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.domain.DomainValidationException;

/** rating's copy of one line of a match sheet: a player who played and can be rated (RF-18). */
public record SheetPlayer(UUID playerId, Position position) {

    public SheetPlayer {
        if (playerId == null) {
            throw new DomainValidationException("Player is required");
        }
        if (position == null) {
            throw new DomainValidationException("Player position is required");
        }
    }
}
