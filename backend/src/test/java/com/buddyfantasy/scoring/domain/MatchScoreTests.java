package com.buddyfantasy.scoring.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.scoring.ManagerScoreData;
import com.buddyfantasy.scoring.MatchdayScored;
import com.buddyfantasy.scoring.PlayerScoreData;
import com.buddyfantasy.shared.domain.DomainEvent;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import static com.buddyfantasy.datadefinition.Position.*;
import static com.buddyfantasy.datadefinition.ScoringAction.GOAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scoring a match from its sheet and locked lineups, in any order and at least once (RF-15, RF-30, RF-31), with the
 * table frozen when first scored and a recalculation after a correction (RF-16).
 */
class MatchScoreTests {

    static final ScoringTable DEFAULTS = TestScoringTables.defaults();
    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID MATCH = UUID.randomUUID();
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant LOCKED = KICKOFF.minus(Duration.ofHours(2));
    static final Instant CLOSED = KICKOFF.plus(Duration.ofHours(3));
    static final Instant NOW = CLOSED.plus(Duration.ofSeconds(5));

    final UUID ana = UUID.randomUUID();
    final UUID brais = UUID.randomUUID();
    final UUID keeper = UUID.randomUUID();
    final UUID striker = UUID.randomUUID();
    final UUID benched = UUID.randomUUID();

    static MatchScore match() {
        return MatchScore.create(MATCH, LEAGUE, 4, KICKOFF);
    }

    static PlayerPerformance goals(UUID playerId, Position position, int goals) {
        return new PlayerPerformance(playerId, position, goals, 0, 0, 0, 0, 0, false, false, false);
    }

    /** Keeper: 1 goal (+6). Striker: 2 goals (+6). */
    List<PlayerPerformance> sheet() {
        return List.of(goals(keeper, GOALKEEPER, 1), goals(striker, FORWARD, 2));
    }

    /** Ana: keeper and striker (12). Brais: striker and a player who did not play (6). */
    List<ManagerLineup> lineups() {
        return List.of(new ManagerLineup(ana, List.of(keeper, striker), false),
                new ManagerLineup(brais, List.of(striker, benched), false));
    }

    static Supplier<ScoringTable> table(ScoringTable table) {
        return () -> table;
    }

    /** The only event published, which must be {@link MatchdayScored}. */
    static MatchdayScored scoredEvent(MatchScore match) {
        List<DomainEvent> events = match.pullEvents();
        assertThat(events).singleElement().isInstanceOf(MatchdayScored.class);
        return (MatchdayScored) events.getFirst();
    }

    @Test
    void sheetThenLineupsScoresPlayersAndManagersAndPublishesMatchdayScored() {
        MatchScore match = match();

        assertThat(match.applySheet(4, KICKOFF, sheet(), CLOSED)).isTrue();
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isFalse();
        assertThat(match.applyLineups(lineups(), LOCKED)).isTrue();
        match.score(table(DEFAULTS), NOW);

        assertThat(match.scored()).isTrue();
        assertThat(match.scoredAt()).isEqualTo(NOW);
        assertThat(match.rules()).isEqualTo(DEFAULTS);
        assertThat(match.playerScores()).containsExactlyInAnyOrder(
                new PlayerScore(keeper, GOALKEEPER, 6, 0), new PlayerScore(striker, FORWARD, 6, 0));
        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 12, false), new ManagerScore(brais, 6, false));
        MatchdayScored event = scoredEvent(match);
        assertThat(event.leagueId()).isEqualTo(LEAGUE);
        assertThat(event.matchId()).isEqualTo(MATCH);
        assertThat(event.matchday()).isEqualTo(4);
        assertThat(event.kickoffAt()).isEqualTo(KICKOFF);
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.players()).containsExactlyInAnyOrder(new PlayerScoreData(keeper, GOALKEEPER, 6, 0, 6),
                new PlayerScoreData(striker, FORWARD, 6, 0, 6));
        assertThat(event.managers()).containsExactlyInAnyOrder(new ManagerScoreData(ana, 12, false),
                new ManagerScoreData(brais, 6, false));
    }

    @Test
    void lineupsThenSheetScoresTheSame() {
        MatchScore match = match();

        match.applyLineups(lineups(), LOCKED);
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isFalse();
        assertThat(match.pullEvents()).isEmpty();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.score(table(DEFAULTS), NOW);

        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 12, false), new ManagerScore(brais, 6, false));
        assertThat(match.pullEvents()).singleElement().isInstanceOf(MatchdayScored.class);
    }

    @Test
    void withOnlyOneOfTheCopiesItDoesNotScoreNorReadTheTable() {
        AtomicInteger reads = new AtomicInteger();
        Supplier<ScoringTable> counted = () -> {
            reads.incrementAndGet();
            return DEFAULTS;
        };
        MatchScore onlySheet = match();
        onlySheet.applySheet(4, KICKOFF, sheet(), CLOSED);
        MatchScore onlyLineups = match();
        onlyLineups.applyLineups(lineups(), LOCKED);

        onlySheet.score(counted, NOW);
        onlyLineups.score(counted, NOW);

        assertThat(onlySheet.scored()).isFalse();
        assertThat(onlyLineups.scored()).isFalse();
        assertThat(onlySheet.playerScores()).isEmpty();
        assertThat(onlySheet.pullEvents()).isEmpty();
        assertThat(onlyLineups.pullEvents()).isEmpty();
        assertThat(reads).hasValue(0);
    }

    @Test
    void aLineupOverBudgetScoresZeroButItsPlayersStillScore() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(List.of(new ManagerLineup(ana, List.of(keeper, striker), true)), LOCKED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.managerScores()).containsExactly(new ManagerScore(ana, 0, true));
        assertThat(match.playerScores()).extracting(PlayerScore::points).containsExactly(6, 6);
    }

    @Test
    void aLinedUpPlayerWhoDidNotPlayScoresZeroAndAnEmptyLineupScoresZero() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(List.of(new ManagerLineup(ana, List.of(benched), false),
                new ManagerLineup(brais, List.of(), false)), LOCKED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 0, false), new ManagerScore(brais, 0, false));
        assertThat(match.playerScores()).extracting(PlayerScore::playerId).doesNotContain(benched);
    }

    @Test
    void withoutLineupsLockedTheMatchIsScoredWithNoManagers() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(List.of(), LOCKED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.scored()).isTrue();
        assertThat(match.managerScores()).isEmpty();
        assertThat(scoredEvent(match).managers()).isEmpty();
    }

    @Test
    void scoringTheSameDataAgainChangesNothingAndPublishesNothing() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();

        assertThat(match.applySheet(4, KICKOFF, sheet(), CLOSED)).isFalse();
        assertThat(match.applyLineups(lineups(), LOCKED)).isFalse();
        match.score(table(DEFAULTS), NOW.plusSeconds(60));

        assertThat(match.scoredAt()).isEqualTo(NOW);
        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    void aCorrectionThatChangesPointsRecalculatesAndPublishesAgain() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();
        Instant corrected = NOW.plus(Duration.ofHours(1));

        assertThat(match.applySheet(4, KICKOFF, List.of(goals(keeper, GOALKEEPER, 1), goals(striker, FORWARD, 3)),
                corrected)).isTrue();
        match.score(table(DEFAULTS), corrected);

        assertThat(match.scoredAt()).isEqualTo(corrected);
        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 15, false), new ManagerScore(brais, 9, false));
        MatchdayScored event = scoredEvent(match);
        assertThat(event.occurredAt()).isEqualTo(corrected);
        assertThat(event.players()).contains(new PlayerScoreData(striker, FORWARD, 9, 0, 9));
    }

    @Test
    void aCorrectionThatOnlyAddsAPlayerWithoutPointsIsPublished() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();
        List<PlayerPerformance> withBenched = List.of(goals(keeper, GOALKEEPER, 1), goals(striker, FORWARD, 2),
                goals(benched, DEFENDER, 0));

        match.applySheet(4, KICKOFF, withBenched, NOW.plusSeconds(60));
        match.score(table(DEFAULTS), NOW.plusSeconds(60));

        assertThat(scoredEvent(match).players()).hasSize(3);
    }

    @Test
    void theRecalculationUsesTheFrozenTableAndDoesNotReadTheCurrentOne() {
        ScoringTable tenPerGoal = TestScoringTables.withPoints(DEFAULTS, GOAL, 10);
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.score(table(tenPerGoal), NOW);
        match.pullEvents();
        Instant corrected = NOW.plus(Duration.ofHours(1));

        match.applySheet(4, KICKOFF, List.of(goals(keeper, GOALKEEPER, 1), goals(striker, FORWARD, 3)), corrected);
        match.score(() -> {
            throw new AssertionError("The frozen table must be used");
        }, corrected);

        assertThat(match.rules()).isEqualTo(tenPerGoal);
        assertThat(match.playerScores()).containsExactlyInAnyOrder(
                new PlayerScore(keeper, GOALKEEPER, 10, 0), new PlayerScore(striker, FORWARD, 30, 0));
    }

    @Test
    void anOlderSheetDeliveredAgainAfterACorrectionIsIgnored() {
        MatchScore match = match();
        Instant corrected = CLOSED.plus(Duration.ofHours(1));
        List<PlayerPerformance> correction = List.of(goals(keeper, GOALKEEPER, 0));
        match.applySheet(4, KICKOFF, correction, corrected);

        assertThat(match.applySheet(4, KICKOFF, sheet(), CLOSED)).isFalse();

        assertThat(match.sheet()).isEqualTo(correction);
        assertThat(match.sheetAt()).isEqualTo(corrected);
    }

    @Test
    void theSheetBringsTheMatchdayAndKickoffOfTheClosedMatch() {
        MatchScore lockedBeforeAPostponement = match();
        Instant postponed = KICKOFF.plus(Duration.ofMinutes(30));

        lockedBeforeAPostponement.applySheet(5, postponed, sheet(), CLOSED);

        assertThat(lockedBeforeAPostponement.matchday()).isEqualTo(5);
        assertThat(lockedBeforeAPostponement.kickoffAt()).isEqualTo(postponed);
    }

    @Test
    void anUnlockDropsTheLineupsLockedBeforeItAndIgnoresThemIfDeliveredAgain() {
        MatchScore match = match();
        Instant unlocked = LOCKED.plus(Duration.ofMinutes(10));
        match.applyLineups(lineups(), LOCKED);

        assertThat(match.unlock(unlocked)).isTrue();

        assertThat(match.lineups()).isEmpty();
        assertThat(match.lineupsAt()).isNull();
        assertThat(match.unlockedAt()).isEqualTo(unlocked);
        assertThat(match.applyLineups(lineups(), LOCKED)).isFalse();
        assertThat(match.unlock(unlocked)).isFalse();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isFalse();

        Instant relocked = unlocked.plus(Duration.ofDays(7));
        assertThat(match.applyLineups(lineups(), relocked)).isTrue();
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isTrue();
    }

    @Test
    void anOlderUnlockDeliveredLateKeepsTheNewerLineups() {
        MatchScore match = match();
        Instant unlocked = LOCKED.plus(Duration.ofMinutes(10));
        Instant relocked = unlocked.plus(Duration.ofDays(7));
        match.applyLineups(lineups(), relocked);

        match.unlock(unlocked);

        assertThat(match.lineups()).isEqualTo(lineups());
        assertThat(match.lineupsAt()).isEqualTo(relocked);
        assertThat(match.applyLineups(lineups(), LOCKED)).isFalse();
    }

    @Test
    void aScoredMatchIgnoresAnUnlockAndKeepsItsPoints() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.score(table(DEFAULTS), NOW);

        assertThat(match.unlock(NOW.plusSeconds(60))).isFalse();

        assertThat(match.lineups()).hasSize(2);
        assertThat(match.managerScores()).hasSize(2);
    }

    @Test
    void aPlayerOrAManagerAppearsOnce() {
        MatchScore match = match();

        assertThatThrownBy(() -> match.applySheet(4, KICKOFF,
                List.of(goals(keeper, GOALKEEPER, 1), goals(keeper, GOALKEEPER, 0)), CLOSED))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> match.applyLineups(List.of(new ManagerLineup(ana, List.of(keeper), false),
                new ManagerLineup(ana, List.of(striker), false)), LOCKED))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new ManagerLineup(ana, List.of(keeper, keeper), false))
                .isInstanceOf(DomainValidationException.class);
        assertThat(match.hasSheet()).isFalse();
    }
}
