package com.buddyfantasy.rating.application.usecases.listratingwindows;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.rating.domain.RatingStatus;

/**
 * A closed match's rating window at the server's {@code now}: until when it accepts votes, how many players can be
 * rated and how many of them the member rated.
 */
public record RatingWindowView(UUID matchId, int matchday, Instant kickoffAt, RatingStatus status, Instant closesAt,
                               int players, int myRatings) {
}
