package com.buddyfantasy.match.domain;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.MatchClosed;
import com.buddyfantasy.match.MatchCorrected;
import com.buddyfantasy.match.MatchDeleted;
import com.buddyfantasy.match.MatchRescheduled;
import com.buddyfantasy.shared.domain.AggregateRoot;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A match of the league's real team (RF-12 to RF-16); 1 matchday = 1 match. {@code leagueId} is a logical reference
 * to the league module. The admin records the sheet and closes the match explicitly; a closed match can still have
 * its sheet corrected (RF-16) and its informative details edited, but not its matchday or kickoff, and it is never
 * deleted. Every deadline uses the {@code now} given by the caller (server clock).
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class Match extends AggregateRoot {

    public static final int MAX_OPPONENT_LENGTH = 100;

    private final MatchId id;
    private final UUID leagueId;
    private Matchday matchday;
    private String opponent;
    private Venue venue;
    private Instant kickoffAt;
    private MatchStatus status;
    private MatchSheet sheet;
    private final Instant createdAt;
    private Instant closedAt;

    /** Kickoff may be in the past, so the calendar can be loaded mid-season. Starts with an empty sheet. */
    public static Match schedule(UUID leagueId, Matchday matchday, String opponent, Venue venue, Instant kickoffAt,
            Instant now) {
        validateDetails(opponent, venue);
        validateSchedule(matchday, kickoffAt);
        return new Match(MatchId.generate(), leagueId, matchday, opponent, venue, kickoffAt, MatchStatus.SCHEDULED,
                MatchSheet.EMPTY, now, null);
    }

    /** Opponent and venue are informative only, so they can change in any status. */
    public void updateDetails(String newOpponent, Venue newVenue) {
        validateDetails(newOpponent, newVenue);
        opponent = newOpponent;
        venue = newVenue;
    }

    /**
     * Changes matchday and kickoff (postponement) and publishes {@link MatchRescheduled}. Without changes it does
     * nothing, in any status; changing a closed match throws {@link ConflictException}.
     */
    public void reschedule(Matchday newMatchday, Instant newKickoffAt, Instant now) {
        validateSchedule(newMatchday, newKickoffAt);
        if (matchday.equals(newMatchday) && kickoffAt.equals(newKickoffAt)) {
            return;
        }
        if (isClosed()) {
            throw new ConflictException("The matchday and kickoff of a closed match cannot change");
        }
        Instant previousKickoffAt = kickoffAt;
        matchday = newMatchday;
        kickoffAt = newKickoffAt;
        register(new MatchRescheduled(leagueId, id.value(), matchday.value(), previousKickoffAt, kickoffAt, now));
    }

    /**
     * Replaces the whole sheet. On a closed match it is a correction (RF-16): the sheet must stay complete and
     * {@link MatchCorrected} is published. The same sheet again does nothing.
     */
    public void recordSheet(MatchSheet newSheet, Instant now) {
        if (newSheet == null) {
            throw new DomainValidationException("Match sheet is required");
        }
        if (sheet.equals(newSheet)) {
            return;
        }
        if (isClosed() && !newSheet.isComplete()) {
            throw new ConflictException("A closed match needs a result and at least one player");
        }
        sheet = newSheet;
        if (isClosed()) {
            register(new MatchCorrected(leagueId, id.value(), matchday.value(), sheet.performancesData(), now));
        }
    }

    /**
     * Requires the match to have started ({@code now >= kickoffAt}) and a complete sheet; publishes
     * {@link MatchClosed}. Idempotent: closing twice keeps the first instant and publishes a single event.
     */
    public void close(Instant now) {
        if (isClosed()) {
            return;
        }
        if (now.isBefore(kickoffAt)) {
            throw new ConflictException("A match cannot be closed before kickoff");
        }
        if (!sheet.isComplete()) {
            throw new ConflictException("A match needs a result and at least one player to be closed");
        }
        status = MatchStatus.CLOSED;
        closedAt = now;
        register(new MatchClosed(leagueId, id.value(), matchday.value(), kickoffAt, sheet.performancesData(), now));
    }

    /** Only a scheduled match can be deleted (suspended and not replayed); publishes {@link MatchDeleted}. */
    public void delete(Instant now) {
        if (isClosed()) {
            throw new ConflictException("A closed match cannot be deleted");
        }
        register(new MatchDeleted(leagueId, id.value(), matchday.value(), now));
    }

    public boolean isClosed() {
        return status == MatchStatus.CLOSED;
    }

    private static void validateDetails(String opponent, Venue venue) {
        if (opponent == null || opponent.isBlank() || opponent.length() > MAX_OPPONENT_LENGTH) {
            throw new DomainValidationException("Opponent must be 1-" + MAX_OPPONENT_LENGTH + " characters");
        }
        if (venue == null) {
            throw new DomainValidationException("Venue is required");
        }
    }

    private static void validateSchedule(Matchday matchday, Instant kickoffAt) {
        if (matchday == null) {
            throw new DomainValidationException("Matchday is required");
        }
        if (kickoffAt == null) {
            throw new DomainValidationException("Kickoff is required");
        }
    }
}
