package com.buddyfantasy.rating.application.usecases.listratingwindows;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * RF-17, RF-18: the rating windows of the league's closed matches for a member, open ones first and then the most
 * recent kickoff first, with how many players the member rated in each: for the «pending ratings» notice.
 */
public record ListRatingWindowsQuery(UUID userId, UUID leagueId) implements Query<List<RatingWindowView>> {
}
