package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.LineupsLocked;
import com.buddyfantasy.lineup.LineupsUnlocked;
import com.buddyfantasy.lineup.LockedLineupData;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locking a match's lineups with the budget check at the lock (RF-25, RF-27, RF-29), and unlocking on postponement. */
class MatchLineupLockTests {

    static final UUID LEAGUE = UUID.randomUUID();
    static final int LOCK_HOURS = 2;
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant LOCK_AT = KICKOFF.minus(Duration.ofHours(LOCK_HOURS));
    static final Instant SAVED = LOCK_AT.minus(Duration.ofDays(1));
    static final MatchSchedule MATCH = new MatchSchedule(UUID.randomUUID(), LEAGUE, 4, KICKOFF, false);

    static LineupSettings settings(boolean budgetEnabled, String budget) {
        return new LineupSettings(2, LOCK_HOURS, 2, false, 0, 0, 0, 0, budgetEnabled, new BigDecimal(budget));
    }

    final Map<UUID, LineupPlayer> players = new HashMap<>();

    UUID player(String value, boolean active) {
        UUID id = UUID.randomUUID();
        players.put(id, new LineupPlayer(id, Position.FORWARD, active, new BigDecimal(value)));
        return id;
    }

    static LineupVersion version(UUID userId, Instant savedAt, UUID... playerIds) {
        return LineupVersion.save(LEAGUE, userId, List.of(playerIds), savedAt);
    }

    @Test
    @DisplayName("Locks each lineup with its values and publishes LineupsLocked")
    void locksEachLineupWithItsValuesAndPublishesLineupsLocked() {
        UUID ana = UUID.randomUUID();
        UUID brais = UUID.randomUUID();
        UUID cheap = player("10", true);
        UUID retired = player("15", false);
        UUID star = player("40", true);
        LineupVersion anaLineup = version(ana, SAVED, cheap, retired);
        LineupVersion braisLineup = version(brais, SAVED, cheap, star);
        Instant now = LOCK_AT.plus(Duration.ofMinutes(1));

        MatchLineupLock lock = MatchLineupLock.lock(MATCH, settings(true, "40"), List.of(anaLineup, braisLineup),
                players, now);

        assertThat(lock.matchId()).isEqualTo(MATCH.id());
        assertThat(lock.leagueId()).isEqualTo(LEAGUE);
        assertThat(lock.matchday()).isEqualTo(4);
        assertThat(lock.kickoffAt()).isEqualTo(KICKOFF);
        assertThat(lock.lockAt()).isEqualTo(LOCK_AT);
        assertThat(lock.lockedAt()).isEqualTo(now);
        assertThat(lock.lineups()).hasSize(2);
        LockedLineup anaLocked = lock.lineups().getFirst();
        assertThat(anaLocked.userId()).isEqualTo(ana);
        assertThat(anaLocked.versionId()).isEqualTo(anaLineup.id());
        assertThat(anaLocked.players()).containsExactlyInAnyOrder(
                new LockedPlayer(cheap, new BigDecimal("10")), new LockedPlayer(retired, new BigDecimal("15")));
        assertThat(anaLocked.totalValue()).isEqualByComparingTo("25");
        assertThat(anaLocked.budget()).isEqualByComparingTo("40");
        assertThat(anaLocked.overBudget()).isFalse();
        assertThat(lock.lineups().get(1).overBudget()).isTrue();

        assertThat(lock.pullEvents()).singleElement().isInstanceOfSatisfying(LineupsLocked.class, event -> {
            assertThat(event.leagueId()).isEqualTo(LEAGUE);
            assertThat(event.matchId()).isEqualTo(MATCH.id());
            assertThat(event.matchday()).isEqualTo(4);
            assertThat(event.kickoffAt()).isEqualTo(KICKOFF);
            assertThat(event.occurredAt()).isEqualTo(now);
            assertThat(event.lineups()).extracting(LockedLineupData::userId).containsExactly(ana, brais);
            assertThat(event.lineups().getFirst().playerIds()).containsExactlyInAnyOrder(cheap, retired);
            assertThat(event.lineups().getFirst().totalValue()).isEqualByComparingTo("25");
            assertThat(event.lineups()).extracting(LockedLineupData::overBudget).containsExactly(false, true);
        });
    }

    @Test
    @DisplayName("Exactly the budget is not over budget")
    void exactlyTheBudgetIsNotOverBudget() {
        UUID a = player("20", true);
        UUID b = player("20", true);

        MatchLineupLock lock = MatchLineupLock.lock(MATCH, settings(true, "40"),
                List.of(version(UUID.randomUUID(), SAVED, a, b)), players, LOCK_AT);

        assertThat(lock.lineups().getFirst().overBudget()).isFalse();
    }

    @Test
    @DisplayName("With the budget disabled nothing is over budget and no budget is kept")
    void withTheBudgetDisabledNothingIsOverBudgetAndNoBudgetIsKept() {
        UUID a = player("500", true);
        UUID b = player("500", true);

        MatchLineupLock lock = MatchLineupLock.lock(MATCH, settings(false, "40"),
                List.of(version(UUID.randomUUID(), SAVED, a, b)), players, LOCK_AT);

        assertThat(lock.lineups().getFirst().overBudget()).isFalse();
        assertThat(lock.lineups().getFirst().budget()).isNull();
    }

    @Test
    @DisplayName("Without lineups the match is locked empty")
    void withoutLineupsTheMatchIsLockedEmpty() {
        MatchLineupLock lock = MatchLineupLock.lock(MATCH, settings(true, "40"), List.of(), players, LOCK_AT);

        assertThat(lock.lineups()).isEmpty();
        assertThat(lock.pullEvents()).singleElement().isInstanceOfSatisfying(LineupsLocked.class,
                event -> assertThat(event.lineups()).isEmpty());
    }

    @Test
    @DisplayName("Cannot lock before the lock time")
    void cannotLockBeforeTheLockTime() {
        assertThatThrownBy(() -> MatchLineupLock.lock(MATCH, settings(true, "40"), List.of(), players,
                LOCK_AT.minusMillis(1))).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Rejects lineups saved at or after the lock")
    void rejectsLineupsSavedAtOrAfterTheLock() {
        UUID a = player("1", true);
        UUID b = player("1", true);

        assertThatThrownBy(() -> MatchLineupLock.lock(MATCH, settings(true, "40"),
                List.of(version(UUID.randomUUID(), LOCK_AT, a, b)), players, LOCK_AT))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Rejects lineups of another league or two for the same user")
    void rejectsLineupsOfAnotherLeagueOrTwoForTheSameUser() {
        UUID a = player("1", true);
        UUID b = player("1", true);
        UUID user = UUID.randomUUID();
        LineupVersion foreign = LineupVersion.save(UUID.randomUUID(), user, List.of(a, b), SAVED);

        assertThatThrownBy(() -> MatchLineupLock.lock(MATCH, settings(true, "40"), List.of(foreign), players,
                LOCK_AT)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> MatchLineupLock.lock(MATCH, settings(true, "40"),
                List.of(version(user, SAVED, a, b), version(user, SAVED.plusSeconds(1), a, b)), players, LOCK_AT))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Rejects lineups with players missing from the squad")
    void rejectsLineupsWithPlayersMissingFromTheSquad() {
        UUID a = player("1", true);

        assertThatThrownBy(() -> MatchLineupLock.lock(MATCH, settings(true, "40"),
                List.of(version(UUID.randomUUID(), SAVED, a, UUID.randomUUID())), players, LOCK_AT))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("A postponement with the new lock in the future unlocks")
    void aPostponementWithTheLockInTheFutureUnlocks() {
        MatchLineupLock lock = MatchLineupLock.lock(MATCH, settings(true, "40"), List.of(), players, LOCK_AT);
        lock.pullEvents();
        Instant now = KICKOFF.minus(Duration.ofHours(1));
        Instant newKickoff = now.plus(Duration.ofHours(LOCK_HOURS)).plusMillis(1);

        assertThat(lock.reschedule(newKickoff, LOCK_HOURS, now)).isTrue();
        assertThat(lock.pullEvents()).containsExactly(new LineupsUnlocked(LEAGUE, MATCH.id(), now));
    }

    @Test
    @DisplayName("A postponement with the lock still past changes nothing")
    void aPostponementWithTheLockStillPastChangesNothing() {
        MatchLineupLock lock = MatchLineupLock.lock(MATCH, settings(true, "40"), List.of(), players, LOCK_AT);
        lock.pullEvents();
        Instant now = KICKOFF.minus(Duration.ofHours(1));
        Instant newKickoff = now.plus(Duration.ofHours(LOCK_HOURS));

        assertThat(lock.reschedule(newKickoff, LOCK_HOURS, now)).isFalse();
        assertThat(lock.pullEvents()).isEmpty();
    }
}
