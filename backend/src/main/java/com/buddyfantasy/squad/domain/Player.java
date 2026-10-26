package com.buddyfantasy.squad.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.domain.AggregateRoot;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.squad.PlayerRegistered;
import com.buddyfantasy.squad.PlayerRetired;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A player of the league's real team (RF-09, RF-10). {@code leagueId} is a logical reference to the league module.
 * Retiring is a soft delete: the player leaves the active squad but stays in past matches, points and lineups.
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class Player extends AggregateRoot {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_PHOTO_KEY_LENGTH = 200;
    /** Matches {@code NUMERIC(10, 2)}: values in millions with up to 8 integer digits and 2 decimals. */
    private static final int VALUE_SCALE = 2;
    private static final int VALUE_INTEGER_DIGITS = 8;

    private final PlayerId id;
    private final UUID leagueId;
    private String name;
    private ShirtNumber shirtNumber;
    private Position position;
    private BigDecimal initialValue;
    private String photoKey;
    private final Instant createdAt;
    private Instant retiredAt;

    public static Player register(UUID leagueId, String name, ShirtNumber shirtNumber, Position position,
            BigDecimal initialValue, String photoKey, Instant now) {
        validate(name, position, initialValue, photoKey);
        Player player = new Player(PlayerId.generate(), leagueId, name, shirtNumber, position, initialValue,
                photoKey, now, null);
        player.register(new PlayerRegistered(leagueId, player.id.value(), initialValue, now));
        return player;
    }

    /** Replaces every editable field. A retired player can no longer be edited. */
    public void update(String newName, ShirtNumber newShirtNumber, Position newPosition, BigDecimal newInitialValue,
            String newPhotoKey) {
        if (isRetired()) {
            throw new ConflictException("A retired player cannot be edited");
        }
        validate(newName, newPosition, newInitialValue, newPhotoKey);
        name = newName;
        shirtNumber = newShirtNumber;
        position = newPosition;
        initialValue = newInitialValue;
        photoKey = newPhotoKey;
    }

    /** Idempotent: retiring twice keeps the first instant and publishes a single event. */
    public void retire(Instant now) {
        if (retiredAt == null) {
            retiredAt = now;
            register(new PlayerRetired(leagueId, id.value(), now));
        }
    }

    public boolean isRetired() {
        return retiredAt != null;
    }

    private static void validate(String name, Position position, BigDecimal initialValue, String photoKey) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new DomainValidationException("Player name must be 1-" + MAX_NAME_LENGTH + " characters");
        }
        if (position == null) {
            throw new DomainValidationException("Player position is required");
        }
        if (initialValue == null || initialValue.signum() <= 0) {
            throw new DomainValidationException("Player initial value must be > 0");
        }
        BigDecimal normalized = initialValue.stripTrailingZeros();
        if (normalized.scale() > VALUE_SCALE || normalized.precision() - normalized.scale() > VALUE_INTEGER_DIGITS) {
            throw new DomainValidationException("Player initial value must have at most "
                    + VALUE_INTEGER_DIGITS + " integer digits and " + VALUE_SCALE + " decimals");
        }
        if (photoKey != null && (photoKey.isBlank() || photoKey.length() > MAX_PHOTO_KEY_LENGTH)) {
            throw new DomainValidationException("Photo key must be 1-" + MAX_PHOTO_KEY_LENGTH + " characters");
        }
    }
}
