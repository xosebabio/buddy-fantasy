package com.buddyfantasy.rating.application.usecases.openratingwindow;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.rating.domain.SheetPlayer;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-17: a match was closed ({@code MatchClosed}) at {@code occurredAt} with this sheet, so its rating window opens
 * then with the league's current rating parameters. Idempotent: returns true only for the call that opened it; false
 * if the match already has a window, also one opened by a {@code MatchCorrected} delivered first.
 * {@code occurredAt} is kept to the microsecond, as stored, so the same event delivered again is never newer.
 */
public record OpenRatingWindowCommand(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<SheetPlayer> sheet, Instant occurredAt) implements Command<Boolean> {

    public OpenRatingWindowCommand {
        sheet = List.copyOf(sheet);
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }

    public MatchSchedule match() {
        return new MatchSchedule(matchId, leagueId, matchday, kickoffAt, true);
    }
}
