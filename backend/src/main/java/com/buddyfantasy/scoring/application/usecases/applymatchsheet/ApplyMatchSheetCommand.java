package com.buddyfantasy.scoring.application.usecases.applymatchsheet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.domain.PlayerPerformance;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-15: a match was closed with this sheet. scoring keeps its copy, unless it already has one from a more recent
 * event, and scores the match if it also has its locked lineups. Idempotent: applying it again changes nothing.
 * {@code occurredAt} is kept to the microsecond, as stored, so the same event delivered again is never newer.
 */
public record ApplyMatchSheetCommand(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<PlayerPerformance> performances, Instant occurredAt) implements Command<Void> {

    public ApplyMatchSheetCommand {
        performances = List.copyOf(performances);
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }
}
