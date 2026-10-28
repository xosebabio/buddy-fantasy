package com.buddyfantasy.match.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.match.MatchClosed;
import com.buddyfantasy.match.MatchCorrected;
import com.buddyfantasy.match.MatchDeleted;
import com.buddyfantasy.match.MatchRescheduled;
import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.match.adapter.out.config.TestMatchLimits;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchTests {

    static final UUID LEAGUE = UUID.randomUUID();
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant CREATED = KICKOFF.minus(Duration.ofDays(7));
    static final MatchLimits LIMITS = TestMatchLimits.get();

    static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    static Match schedule() {
        return Match.schedule(LEAGUE, new Matchday(5), "Os Ninguéns", Venue.HOME, KICKOFF, CREATED, LIMITS);
    }

    static PlayerPerformance scorer(UUID playerId, Position position, int goals) {
        return new PlayerPerformance(playerId, position, goals, 0, 0, 0, 0, 0, false, false, false);
    }

    static MatchSheet completeSheet() {
        return new MatchSheet(new MatchResult(2, 1), List.of(
                scorer(UUID.randomUUID(), Position.FORWARD, 2), scorer(UUID.randomUUID(), Position.GOALKEEPER, 0)));
    }

    static Match closed(MatchSheet sheet) {
        Match match = schedule();
        match.recordSheet(sheet, CREATED, LIMITS);
        match.close(at(KICKOFF).instant());
        match.pullEvents();
        return match;
    }

    @Test
    @DisplayName("schedule creates a scheduled match with an empty sheet and no events")
    void scheduleCreatesAScheduledMatchWithAnEmptySheetAndNoEvents() {
        Match match = schedule();

        assertThat(match.status()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(match.isClosed()).isFalse();
        assertThat(match.leagueId()).isEqualTo(LEAGUE);
        assertThat(match.matchday()).isEqualTo(new Matchday(5));
        assertThat(match.kickoffAt()).isEqualTo(KICKOFF);
        assertThat(match.createdAt()).isEqualTo(CREATED);
        assertThat(match.closedAt()).isNull();
        assertThat(match.sheet()).isEqualTo(MatchSheet.EMPTY);
        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("The kickoff may be in the past")
    void kickoffMayBeInThePast() {
        Match match = Match.schedule(LEAGUE, new Matchday(1), "Rival", Venue.AWAY, CREATED.minusSeconds(1), CREATED,
                LIMITS);

        assertThat(match.kickoffAt()).isBefore(match.createdAt());
    }

    @ParameterizedTest
    @DisplayName("Rejects a blank opponent")
    @ValueSource(strings = {"", "   "})
    void rejectsBlankOpponent(String opponent) {
        assertThatThrownBy(() -> Match.schedule(LEAGUE, new Matchday(1), opponent, Venue.HOME, KICKOFF, CREATED,
                LIMITS)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Validates opponent, venue, matchday and kickoff")
    void validatesOpponentVenueMatchdayAndKickoff() {
        assertThatThrownBy(() -> Match.schedule(LEAGUE, new Matchday(1), null, Venue.HOME, KICKOFF, CREATED, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Match.schedule(LEAGUE, new Matchday(1), "x".repeat(101), Venue.HOME, KICKOFF,
                CREATED, LIMITS)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Match.schedule(LEAGUE, new Matchday(1), "Rival", null, KICKOFF, CREATED, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Match.schedule(LEAGUE, null, "Rival", Venue.HOME, KICKOFF, CREATED, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Match.schedule(LEAGUE, new Matchday(1), "Rival", Venue.HOME, null, CREATED, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThat(Match.schedule(LEAGUE, new Matchday(1), "x".repeat(100), Venue.HOME, KICKOFF, CREATED, LIMITS)
                .opponent()).hasSize(100);
    }

    @ParameterizedTest
    @DisplayName("Rejects matchdays out of the storage range")
    @ValueSource(ints = {-1, 32768})
    void rejectsMatchdaysOutOfStorageRange(int value) {
        assertThatThrownBy(() -> new Matchday(value)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("A matchday below the configured minimum is rejected")
    void matchdayBelowTheConfiguredMinimumIsRejected() {
        assertThatThrownBy(() -> Match.schedule(LEAGUE, new Matchday(0), "Rival", Venue.HOME, KICKOFF, CREATED,
                LIMITS)).isInstanceOf(DomainValidationException.class);
        Match match = schedule();
        assertThatThrownBy(() -> match.reschedule(new Matchday(0), KICKOFF, CREATED, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Updating the details is allowed on a closed match")
    void updateDetailsIsAllowedOnAClosedMatch() {
        Match match = closed(completeSheet());

        match.updateDetails("Os Outros", Venue.AWAY, LIMITS);

        assertThat(match.opponent()).isEqualTo("Os Outros");
        assertThat(match.venue()).isEqualTo(Venue.AWAY);
        assertThatThrownBy(() -> match.updateDetails("", Venue.AWAY, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Rescheduling publishes the change with the previous kickoff")
    void reschedulePublishesTheChangeWithThePreviousKickoff() {
        Match match = schedule();
        Instant postponed = KICKOFF.plus(Duration.ofDays(14));

        match.reschedule(new Matchday(5), postponed, CREATED, LIMITS);

        assertThat(match.kickoffAt()).isEqualTo(postponed);
        assertThat(match.pullEvents()).containsExactly(
                new MatchRescheduled(LEAGUE, match.id().value(), 5, KICKOFF, postponed, CREATED));
    }

    @Test
    @DisplayName("Changing only the matchday also publishes MatchRescheduled")
    void changingOnlyTheMatchdayAlsoPublishesRescheduled() {
        Match match = schedule();

        match.reschedule(new Matchday(6), KICKOFF, CREATED, LIMITS);

        assertThat(match.matchday()).isEqualTo(new Matchday(6));
        assertThat(match.pullEvents()).containsExactly(
                new MatchRescheduled(LEAGUE, match.id().value(), 6, KICKOFF, KICKOFF, CREATED));
    }

    @Test
    @DisplayName("Rescheduling without changes does nothing, even when closed")
    void rescheduleWithoutChangesDoesNothingEvenWhenClosed() {
        Match scheduled = schedule();
        scheduled.reschedule(new Matchday(5), KICKOFF, CREATED, LIMITS);
        Match closed = closed(completeSheet());
        closed.reschedule(new Matchday(5), KICKOFF, CREATED, LIMITS);

        assertThat(scheduled.pullEvents()).isEmpty();
        assertThat(closed.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("A closed match cannot change its matchday or kickoff")
    void closedMatchCannotChangeMatchdayOrKickoff() {
        Match match = closed(completeSheet());

        assertThatThrownBy(() -> match.reschedule(new Matchday(5), KICKOFF.plusSeconds(60), KICKOFF, LIMITS))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> match.reschedule(new Matchday(6), KICKOFF, KICKOFF, LIMITS))
                .isInstanceOf(ConflictException.class);
        assertThat(match.matchday()).isEqualTo(new Matchday(5));
        assertThat(match.kickoffAt()).isEqualTo(KICKOFF);
    }

    @Test
    @DisplayName("Recording the sheet on a scheduled match only saves it")
    void recordSheetOnAScheduledMatchOnlySavesIt() {
        Match match = schedule();
        MatchSheet draft = new MatchSheet(null, List.of(scorer(UUID.randomUUID(), Position.DEFENDER, 0)));

        match.recordSheet(draft, CREATED, LIMITS);

        assertThat(match.sheet()).isEqualTo(draft);
        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("Closing before kickoff is rejected")
    void closeBeforeKickoffIsRejected() {
        Match match = schedule();
        match.recordSheet(completeSheet(), CREATED, LIMITS);

        assertThatThrownBy(() -> match.close(at(KICKOFF.minusMillis(1)).instant()))
                .isInstanceOf(ConflictException.class);
        assertThat(match.isClosed()).isFalse();
    }

    @Test
    @DisplayName("Closing exactly at kickoff closes and publishes the full sheet")
    void closeExactlyAtKickoffClosesAndPublishesTheFullSheet() {
        Match match = schedule();
        MatchSheet sheet = completeSheet();
        match.recordSheet(sheet, CREATED, LIMITS);
        Instant now = at(KICKOFF).instant();

        match.close(now);

        assertThat(match.status()).isEqualTo(MatchStatus.CLOSED);
        assertThat(match.closedAt()).isEqualTo(now);
        List<PlayerPerformanceData> performances = sheet.performances().stream()
                .map(PlayerPerformance::toData).toList();
        assertThat(match.pullEvents()).containsExactly(
                new MatchClosed(LEAGUE, match.id().value(), 5, KICKOFF, performances, now));
    }

    @Test
    @DisplayName("Closing requires a result and at least one player")
    void closeRequiresAResultAndAtLeastOnePlayer() {
        Instant afterKickoff = at(KICKOFF.plusSeconds(3600)).instant();
        Match empty = schedule();
        Match withoutResult = schedule();
        withoutResult.recordSheet(new MatchSheet(null, List.of(scorer(UUID.randomUUID(), Position.FORWARD, 0))),
                CREATED, LIMITS);
        Match withoutPlayers = schedule();
        withoutPlayers.recordSheet(new MatchSheet(new MatchResult(0, 0), List.of()), CREATED, LIMITS);

        assertThatThrownBy(() -> empty.close(afterKickoff)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> withoutResult.close(afterKickoff)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> withoutPlayers.close(afterKickoff)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Closing twice publishes MatchClosed once and keeps the first instant")
    void closingTwicePublishesMatchClosedOnceAndKeepsTheFirstInstant() {
        Match match = schedule();
        match.recordSheet(completeSheet(), CREATED, LIMITS);
        Instant first = at(KICKOFF.plusSeconds(60)).instant();

        match.close(first);
        match.close(at(KICKOFF.plusSeconds(600)).instant());

        assertThat(match.closedAt()).isEqualTo(first);
        assertThat(match.pullEvents()).hasSize(1).first().isInstanceOf(MatchClosed.class);
    }

    @Test
    @DisplayName("Recording the sheet on a closed match is a correction")
    void recordSheetOnAClosedMatchIsACorrection() {
        Match match = closed(completeSheet());
        MatchSheet corrected = new MatchSheet(new MatchResult(2, 1), List.of(
                scorer(UUID.randomUUID(), Position.MIDFIELDER, 2)));
        Instant now = KICKOFF.plus(Duration.ofDays(2));

        match.recordSheet(corrected, now, LIMITS);

        assertThat(match.sheet()).isEqualTo(corrected);
        assertThat(match.pullEvents()).containsExactly(new MatchCorrected(LEAGUE, match.id().value(), 5,
                corrected.performancesData(), now));
    }

    @Test
    @DisplayName("The same sheet on a closed match publishes nothing")
    void theSameSheetOnAClosedMatchPublishesNothing() {
        MatchSheet sheet = completeSheet();
        Match match = closed(sheet);

        match.recordSheet(new MatchSheet(sheet.result(), sheet.performances().reversed()), KICKOFF, LIMITS);

        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("A correction must keep the sheet complete")
    void aCorrectionMustKeepTheSheetComplete() {
        Match match = closed(completeSheet());
        MatchSheet before = match.sheet();

        assertThatThrownBy(() -> match.recordSheet(MatchSheet.EMPTY, KICKOFF, LIMITS))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> match.recordSheet(new MatchSheet(null, before.performances()), KICKOFF, LIMITS))
                .isInstanceOf(ConflictException.class);
        assertThat(match.sheet()).isEqualTo(before);
        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("Recording the sheet requires a sheet")
    void recordSheetRequiresASheet() {
        assertThatThrownBy(() -> schedule().recordSheet(null, CREATED, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Deleting publishes MatchDeleted")
    void deletePublishesMatchDeleted() {
        Match match = schedule();

        match.delete(CREATED);

        assertThat(match.pullEvents()).containsExactly(new MatchDeleted(LEAGUE, match.id().value(), 5, CREATED));
    }

    @Test
    @DisplayName("A closed match cannot be deleted")
    void closedMatchCannotBeDeleted() {
        Match match = closed(completeSheet());

        assertThatThrownBy(() -> match.delete(KICKOFF)).isInstanceOf(ConflictException.class);
        assertThat(match.pullEvents()).isEmpty();
    }
}
