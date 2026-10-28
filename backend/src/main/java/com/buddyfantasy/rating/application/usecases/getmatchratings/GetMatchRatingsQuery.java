package com.buddyfantasy.rating.application.usecases.getmatchratings;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * RF-18 to RF-21: a member (the admin included) sees the players they can rate in a closed match with their own
 * votes, never another member's, not even the admin; once the window is closed, also each player's votes, average
 * and points for ratings. 404 for a match of another league, 409 {@code MATCH_NOT_CLOSED} for one not closed yet.
 */
public record GetMatchRatingsQuery(UUID userId, UUID leagueId, UUID matchId) implements Query<MatchRatingsView> {
}
