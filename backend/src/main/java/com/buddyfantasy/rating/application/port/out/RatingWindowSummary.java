package com.buddyfantasy.rating.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * A rating window of the league for the listing: how many players can be rated in it and how many of them a member
 * rated. {@code closedAt} is null until the job closes it.
 */
public record RatingWindowSummary(UUID matchId, int matchday, Instant kickoffAt, Instant closesAt, Instant closedAt,
                                  int players, int myRatings) {
}
