package com.buddyfantasy.rating.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * RF-41: the admin ({@code excludedBy}) cancelled the ratings of a member ({@code userId}) in a match at
 * {@code excludedAt}. Definitive: the member can no longer rate that match. It is also the audit of who cancelled
 * whom and when.
 */
public record RaterExclusion(UUID matchId, UUID userId, UUID excludedBy, Instant excludedAt) {
}
