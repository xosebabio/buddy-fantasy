package com.buddyfantasy.scoring.application.usecases.applylockedlineups;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.domain.ManagerLineup;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-31: the lineups of a match were locked. scoring keeps its copy, unless it already has one from a more recent
 * event or the lineups were unlocked after {@code occurredAt}, and scores the match if it also has its sheet.
 * Idempotent: applying it again changes nothing.
 * {@code occurredAt} is kept to the microsecond, as stored, so the same event delivered again is never newer.
 */
public record ApplyLockedLineupsCommand(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<ManagerLineup> lineups, Instant occurredAt) implements Command<Void> {

    public ApplyLockedLineupsCommand {
        lineups = List.copyOf(lineups);
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }
}
