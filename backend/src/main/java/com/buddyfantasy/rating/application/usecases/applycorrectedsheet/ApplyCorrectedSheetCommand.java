package com.buddyfantasy.rating.application.usecases.applycorrectedsheet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.rating.domain.SheetPlayer;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-16: the sheet of a closed match was corrected ({@code MatchCorrected}) at {@code occurredAt}. rating replaces its
 * copy of the players that can be rated, unless it already has one from a more recent event: a player removed loses
 * his votes, also with the window closed, which publishes {@code RatingsCorrected} if a result changes. Without a
 * window (its {@code MatchClosed} failed and will be delivered again) it opens it with this sheet. Idempotent:
 * applying it again changes nothing. {@code occurredAt} is kept to the microsecond, as stored, so the same event
 * delivered again is never newer.
 */
public record ApplyCorrectedSheetCommand(UUID leagueId, UUID matchId, int matchday, Instant kickoffAt,
        List<SheetPlayer> sheet, Instant occurredAt) implements Command<Void> {

    public ApplyCorrectedSheetCommand {
        sheet = List.copyOf(sheet);
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }

    public MatchSchedule match() {
        return new MatchSchedule(matchId, leagueId, matchday, kickoffAt, true);
    }
}
