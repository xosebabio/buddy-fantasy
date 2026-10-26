package com.buddyfantasy.squad.application.usecases.getplayer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.squad.domain.Player;

/**
 * Base player card (RF-11). Values are in millions. {@code currentValue} equals {@code initialValue} until the
 * market module exists. {@code retiredAt} is null for active players.
 */
public record PlayerView(UUID id, String name, Integer shirtNumber, Position position, BigDecimal initialValue,
                         BigDecimal currentValue, Instant createdAt, Instant retiredAt) {

    public static PlayerView from(Player player) {
        return new PlayerView(player.id().value(), player.name(),
                player.shirtNumber() == null ? null : player.shirtNumber().value(), player.position(),
                player.initialValue(), player.initialValue(), player.createdAt(), player.retiredAt());
    }
}
