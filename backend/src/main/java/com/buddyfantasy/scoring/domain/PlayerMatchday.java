package com.buddyfantasy.scoring.domain;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.ScoringTable;

/**
 * A scored match in which a player was in the sheet, for their points history (RF-11). {@code rules} is the table the
 * match was scored with.
 */
public record PlayerMatchday(UUID matchId, int matchday, Instant kickoffAt, ScoringTable rules, ScoredLine line) {
}
