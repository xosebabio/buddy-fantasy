package com.buddyfantasy.match;

import java.time.Instant;
import java.util.UUID;

/** A match of the league's calendar as seen by other modules: when it kicks off and whether it is closed. */
public record MatchSchedule(UUID id, UUID leagueId, int matchday, Instant kickoffAt, boolean closed) {
}
