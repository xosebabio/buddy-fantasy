package com.buddyfantasy.squad.domain;

import java.util.UUID;
import java.util.regex.Pattern;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Storage key of a player's photo: {@code leagues/{leagueId}/players/{playerId}/{uuid}.webp}. Every upload gets a
 * new random UUID, so keys are immutable and caches never go stale.
 */
public record PlayerPhotoKey(String value) {

    public static final int MAX_LENGTH = 200;
    private static final String EXTENSION = ".webp";
    private static final Pattern FILE_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" + Pattern.quote(EXTENSION));

    public PlayerPhotoKey {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new DomainValidationException("Photo key must be 1-" + MAX_LENGTH + " characters");
        }
    }

    public static PlayerPhotoKey generate(UUID leagueId, PlayerId playerId) {
        return new PlayerPhotoKey(prefix(leagueId, playerId) + UUID.randomUUID() + EXTENSION);
    }

    /** {@code null} means the player has no photo. */
    public static PlayerPhotoKey ofNullable(String value) {
        return value == null ? null : new PlayerPhotoKey(value);
    }

    /**
     * True only for a key generated for this player: exact prefix plus a single {@code {uuid}.webp} segment, so
     * keys from another league or player, or with extra path segments, are rejected.
     */
    public boolean belongsTo(UUID leagueId, PlayerId playerId) {
        String prefix = prefix(leagueId, playerId);
        return value.startsWith(prefix) && FILE_NAME.matcher(value.substring(prefix.length())).matches();
    }

    private static String prefix(UUID leagueId, PlayerId playerId) {
        return "leagues/" + leagueId + "/players/" + playerId.value() + "/";
    }
}
