package com.buddyfantasy.squad.application.usecases.getplayer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.domain.Player;

/**
 * Base player card (RF-11). Values are in millions. {@code currentValue} equals {@code initialValue} until the
 * market module exists. {@code photoUrl} and {@code retiredAt} are null when there is no photo / for active players.
 */
public record PlayerView(UUID id, String name, Integer shirtNumber, Position position, String photoUrl,
                         BigDecimal initialValue, BigDecimal currentValue, Instant createdAt, Instant retiredAt) {

    /** The URL is built from the stored key on every read, so the CDN can change without touching the data. */
    public static PlayerView from(Player player, ImageStorage images) {
        return new PlayerView(player.id().value(), player.name(),
                player.shirtNumber() == null ? null : player.shirtNumber().value(), player.position(),
                player.photoKey() == null ? null : images.publicUrl(player.photoKey().value()),
                player.initialValue(), player.initialValue(), player.createdAt(), player.retiredAt());
    }
}
