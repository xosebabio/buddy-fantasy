package com.buddyfantasy.league.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Invitation link of a league. Only the token's SHA-256 hash is kept; the raw token is shown once on issue. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class Invitation {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UUID id;
    private final LeagueId leagueId;
    private final String tokenHash;
    private final Instant createdAt;
    private Instant revokedAt;

    /** A freshly created invitation together with its raw token. */
    public record Issued(Invitation invitation, String token) {
    }

    public static Issued issue(LeagueId leagueId, Instant now) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Issued(new Invitation(UUID.randomUUID(), leagueId, hash(token), now, null), token);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public boolean matches(String token) {
        return token != null && MessageDigest.isEqual(
                tokenHash.getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8));
    }

    /** Idempotent. */
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
