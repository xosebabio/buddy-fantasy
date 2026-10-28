package com.buddyfantasy.rating.application.usecases.excluderater;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * RF-41: the admin ({@code userId}) cancels the ratings of a current member of the league ({@code raterId}, the admin
 * included) in a match while its window is open: their votes are deleted and they can no longer rate it, also if they
 * had not rated it yet. Definitive and idempotent: excluding them again changes nothing nor publishes another
 * {@link com.buddyfantasy.rating.RaterExcluded}. 404 for a match of another league or a user who is not a member, 409
 * {@code MATCH_NOT_CLOSED} or {@code RATING_WINDOW_CLOSED}.
 */
public record ExcludeRaterCommand(UUID userId, UUID leagueId, UUID matchId, UUID raterId) implements Command<Void> {
}
