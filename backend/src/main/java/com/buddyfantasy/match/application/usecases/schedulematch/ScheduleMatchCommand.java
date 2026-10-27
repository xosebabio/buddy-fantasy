package com.buddyfantasy.match.application.usecases.schedulematch;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.Command;

/** RF-12: the league admin adds a match to the calendar. Kickoff may be in the past (calendar loaded mid-season). */
public record ScheduleMatchCommand(UUID userId, UUID leagueId, int matchday, String opponent, Venue venue,
                                   Instant kickoffAt) implements Command<MatchId> {
}
