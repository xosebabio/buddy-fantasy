package com.buddyfantasy.rating;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * RF-41: the admin cancelled the ratings of a member ({@code userId}) in a match while its window was open: their
 * votes were deleted and they can no longer rate it. Published once per member and match. No consumers yet:
 * {@code notification} will tell the member (Phase 3); the points do not need it, since the close no longer sees the
 * votes.
 */
public record RaterExcluded(UUID leagueId, UUID matchId, UUID userId, Instant occurredAt) implements DomainEvent {
}
