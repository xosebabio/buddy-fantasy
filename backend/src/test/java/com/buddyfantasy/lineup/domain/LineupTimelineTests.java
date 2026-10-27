package com.buddyfantasy.lineup.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.match.MatchSchedule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Lock, carry-over and baseline over time (RF-25, RF-27, RF-28), with fixed instants as the server clock. */
class LineupTimelineTests {

    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final int LOCK_HOURS = 2;
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant LOCK_1 = KICKOFF_1.minus(Duration.ofHours(LOCK_HOURS));
    static final Instant KICKOFF_2 = KICKOFF_1.plus(Duration.ofDays(7));
    static final Instant LOCK_2 = KICKOFF_2.minus(Duration.ofHours(LOCK_HOURS));
    static final Instant KICKOFF_3 = KICKOFF_2.plus(Duration.ofDays(7));

    static MatchSchedule match(int matchday, Instant kickoffAt, boolean closed) {
        return new MatchSchedule(UUID.randomUUID(), LEAGUE, matchday, kickoffAt, closed);
    }

    static LineupVersion version(Instant savedAt) {
        return new LineupVersion(UUID.randomUUID(), LEAGUE, USER, savedAt, Set.of(UUID.randomUUID()));
    }

    final MatchSchedule first = match(1, KICKOFF_1, false);
    final MatchSchedule second = match(2, KICKOFF_2, false);
    final MatchSchedule third = match(3, KICKOFF_3, false);

    LineupTimeline timeline(List<MatchSchedule> matches, LineupVersion... versions) {
        return new LineupTimeline(matches, LOCK_HOURS, List.of(versions));
    }

    @Test
    void lockAtIsTheKickoffMinusTheLockHours() {
        LineupTimeline timeline = timeline(List.of(first));

        assertThat(timeline.lockAt(first)).isEqualTo(Instant.parse("2026-01-11T08:00:00Z"));
        assertThat(LineupTimeline.lockAt(KICKOFF_1, 0)).isEqualTo(KICKOFF_1);
    }

    @Test
    void aSaveJustBeforeTheLockCountsForThatMatch() {
        LineupVersion early = version(LOCK_1.minus(Duration.ofDays(1)));
        LineupVersion justBefore = version(LOCK_1.minusMillis(1));

        LineupTimeline timeline = timeline(List.of(first, second), early, justBefore);

        assertThat(timeline.lineupFor(first)).contains(justBefore);
    }

    @Test
    void aSaveRightAtTheLockCountsForTheNextMatch() {
        LineupVersion before = version(LOCK_1.minus(Duration.ofHours(1)));
        LineupVersion atLock = version(LOCK_1);

        LineupTimeline timeline = timeline(List.of(first, second), before, atLock);

        assertThat(timeline.lineupFor(first)).contains(before);
        assertThat(timeline.lineupFor(second)).contains(atLock);
        assertThat(timeline.targetMatch(LOCK_1)).contains(second);
    }

    @Test
    void anUntouchedLineupCarriesOverToTheNextMatches() {
        LineupVersion only = version(LOCK_1.minus(Duration.ofDays(1)));

        LineupTimeline timeline = timeline(List.of(first, second, third), only);

        assertThat(timeline.lineupFor(first)).contains(only);
        assertThat(timeline.lineupFor(second)).contains(only);
        assertThat(timeline.lineupFor(third)).contains(only);
    }

    @Test
    void withoutVersionsBeforeTheLockThereIsNoLineupForTheMatch() {
        LineupVersion late = version(LOCK_1.plus(Duration.ofHours(1)));

        LineupTimeline timeline = timeline(List.of(first, second), late);

        assertThat(timeline.lineupFor(first)).isEmpty();
        assertThat(timeline.lineupFor(second)).contains(late);
    }

    @Test
    void aPostponementMovesTheLockAndTheVersionThatCounts() {
        LineupVersion before = version(LOCK_1.minus(Duration.ofDays(1)));
        LineupVersion afterOldLock = version(LOCK_1.plus(Duration.ofDays(1)));
        MatchSchedule postponed = match(1, KICKOFF_1.plus(Duration.ofDays(3)), false);

        assertThat(timeline(List.of(first), before, afterOldLock).lineupFor(first)).contains(before);
        assertThat(timeline(List.of(postponed), before, afterOldLock).lineupFor(postponed)).contains(afterOldLock);
    }

    @Test
    void matchesAreOrderedByKickoffNotByMatchday() {
        // matchday 2 was postponed after matchday 3
        MatchSchedule postponed = match(2, KICKOFF_3, false);
        MatchSchedule played = match(3, KICKOFF_2, false);
        Instant betweenLocks = LOCK_1.plus(Duration.ofDays(1));

        LineupTimeline timeline = timeline(List.of(postponed, first, played));

        assertThat(timeline.targetMatch(betweenLocks)).contains(played);
        assertThat(timeline.lastLockedMatch(LOCK_2)).contains(played);
    }

    @Test
    void targetMatchIsTheFirstNotClosedWhoseLockIsAhead() {
        LineupTimeline timeline = timeline(List.of(first, second));

        assertThat(timeline.targetMatch(LOCK_1.minusMillis(1))).contains(first);
        assertThat(timeline.targetMatch(LOCK_1)).contains(second);
        assertThat(timeline.targetMatch(LOCK_2)).isEmpty();
    }

    @Test
    void aClosedMatchIsNeverTheTarget() {
        MatchSchedule closed = match(1, KICKOFF_1, true);

        assertThat(timeline(List.of(closed, second)).targetMatch(LOCK_1.minus(Duration.ofDays(1))))
                .contains(second);
    }

    @Test
    void baselineIsTheLineupLockedInTheLastLockedMatch() {
        LineupVersion forFirst = version(LOCK_1.minus(Duration.ofDays(1)));
        LineupVersion forSecond = version(LOCK_1.plus(Duration.ofHours(1)));
        LineupTimeline timeline = timeline(List.of(first, second), forFirst, forSecond);

        assertThat(timeline.baseline(LOCK_1.minusMillis(1))).isEmpty();
        assertThat(timeline.baseline(LOCK_1)).contains(forFirst);
        assertThat(timeline.baseline(LOCK_2.minusMillis(1))).contains(forFirst);
        assertThat(timeline.baseline(LOCK_2)).contains(forSecond);
    }

    @Test
    void thereIsNoBaselineIfTheManagerHadNoLineupInTheLastLockedMatch() {
        LineupVersion late = version(LOCK_1.plus(Duration.ofHours(1)));

        assertThat(timeline(List.of(first, second), late).baseline(LOCK_1.plus(Duration.ofHours(2)))).isEmpty();
    }

    @Test
    void currentIsTheLastVersionSavedWhateverTheInputOrder() {
        LineupVersion older = version(LOCK_1.minus(Duration.ofDays(2)));
        LineupVersion newer = version(LOCK_1.plus(Duration.ofDays(2)));

        assertThat(timeline(List.of(first), newer, older).current()).contains(newer);
        assertThat(timeline(List.of(first)).current()).isEmpty();
    }
}
