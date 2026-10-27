package com.buddyfantasy.lineup.application.usecases.findmatchestolock;

import java.util.UUID;

/** A match whose lineups are due to be locked. */
public record MatchToLock(UUID leagueId, UUID matchId) {
}
