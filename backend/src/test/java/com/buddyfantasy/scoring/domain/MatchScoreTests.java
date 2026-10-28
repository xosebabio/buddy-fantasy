package com.buddyfantasy.scoring.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.scoring.ManagerScoreData;
import com.buddyfantasy.scoring.MatchdayScored;
import com.buddyfantasy.scoring.PlayerScoreData;
import com.buddyfantasy.shared.domain.DomainEvent;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.buddyfantasy.datadefinition.Position.*;
import static com.buddyfantasy.datadefinition.ScoringAction.GOAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scoring a match from its sheet, locked lineups and points for ratings, in any order and at least once (RF-15,
 * RF-22, RF-23, RF-30, RF-31), with the table frozen when first scored and a recalculation after a correction of the
 * sheet or of the ratings (RF-16).
 */
class MatchScoreTests {

    static final ScoringTable DEFAULTS = TestScoringTables.defaults();
    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID MATCH = UUID.randomUUID();
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant LOCKED = KICKOFF.minus(Duration.ofHours(2));
    static final Instant CLOSED = KICKOFF.plus(Duration.ofHours(3));
    static final Instant NOW = CLOSED.plus(Duration.ofSeconds(5));
    /** When the rating window closes. */
    static final Instant RATED = CLOSED.plus(Duration.ofHours(24));
    /** The result of a window without votes: every player 0 for ratings. */
    static final Map<UUID, Integer> UNRATED = Map.of();

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

    /** A match with the sheet, the lineups and no votes, not scored yet. */
    MatchScore withAllCopies() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.applyRatings(UNRATED, RATED);
        return match;
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
    @DisplayName("Sheet, lineups and then ratings score players and managers and publish MatchdayScored")
    void sheetLineupsAndThenRatingsScorePlayersAndManagersAndPublishMatchdayScored() {
        MatchScore match = match();

        assertThat(match.applySheet(4, KICKOFF, sheet(), CLOSED)).isTrue();
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isFalse();
        assertThat(match.applyLineups(lineups(), LOCKED)).isTrue();
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isFalse();
        assertThat(match.pullEvents()).isEmpty();
        assertThat(match.applyRatings(UNRATED, RATED)).isTrue();
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

    /** The six orders of the three copies, each one scored after it is applied. */
    @Test
    @DisplayName("The three copies in any order score the same, once")
    void theThreeCopiesInAnyOrderScoreTheSameOnce() {
        List<Consumer<MatchScore>> copies = List.of(
                match -> match.applySheet(4, KICKOFF, sheet(), CLOSED),
                match -> match.applyLineups(lineups(), LOCKED),
                match -> match.applyRatings(Map.of(striker, 2), RATED));
        for (List<Integer> order : List.of(List.of(0, 1, 2), List.of(0, 2, 1), List.of(1, 0, 2), List.of(1, 2, 0),
                List.of(2, 0, 1), List.of(2, 1, 0))) {
            MatchScore match = match();
            List<DomainEvent> events = new ArrayList<>();
            order.forEach(copy -> {
                copies.get(copy).accept(match);
                match.score(table(DEFAULTS), NOW);
                events.addAll(match.pullEvents());
            });

            assertThat(match.playerScores()).as("order %s", order).containsExactlyInAnyOrder(
                    new PlayerScore(keeper, GOALKEEPER, 6, 0), new PlayerScore(striker, FORWARD, 6, 2));
            assertThat(match.managerScores()).as("order %s", order).containsExactlyInAnyOrder(
                    new ManagerScore(ana, 14, false), new ManagerScore(brais, 8, false));
            assertThat(events).as("order %s", order).singleElement().isInstanceOf(MatchdayScored.class);
        }
    }

    @Test
    @DisplayName("With only two of the copies it neither scores nor reads the table")
    void withOnlyTwoOfTheCopiesItDoesNotScoreNorReadTheTable() {
        AtomicInteger reads = new AtomicInteger();
        Supplier<ScoringTable> counted = () -> {
            reads.incrementAndGet();
            return DEFAULTS;
        };
        MatchScore noRatings = match();
        noRatings.applySheet(4, KICKOFF, sheet(), CLOSED);
        noRatings.applyLineups(lineups(), LOCKED);
        MatchScore noLineups = match();
        noLineups.applySheet(4, KICKOFF, sheet(), CLOSED);
        noLineups.applyRatings(UNRATED, RATED);
        MatchScore noSheet = match();
        noSheet.applyLineups(lineups(), LOCKED);
        noSheet.applyRatings(UNRATED, RATED);

        List.of(noRatings, noLineups, noSheet).forEach(match -> {
            match.score(counted, NOW);

            assertThat(match.scored()).isFalse();
            assertThat(match.playerScores()).isEmpty();
            assertThat(match.pullEvents()).isEmpty();
        });
        assertThat(reads).hasValue(0);
    }

    @Test
    @DisplayName("A player's points are those for actions plus those for ratings, without weights")
    void aPlayersPointsAreThoseForActionsPlusThoseForRatings() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        // the keeper was rated badly; a player who did not play is in the window's result too
        match.applyRatings(Map.of(keeper, -2, striker, 14, benched, 6), RATED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.playerScores()).containsExactlyInAnyOrder(
                new PlayerScore(keeper, GOALKEEPER, 6, -2), new PlayerScore(striker, FORWARD, 6, 14));
        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 24, false), new ManagerScore(brais, 20, false));
        assertThat(scoredEvent(match).players()).containsExactlyInAnyOrder(
                new PlayerScoreData(keeper, GOALKEEPER, 6, -2, 4), new PlayerScoreData(striker, FORWARD, 6, 14, 20));
    }

    @Test
    @DisplayName("A player of the sheet missing from the ratings scores zero for ratings")
    void aPlayerOfTheSheetMissingFromTheRatingsScoresZeroForRatings() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(lineups(), LOCKED);
        match.applyRatings(Map.of(striker, 6), RATED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.playerScores()).contains(new PlayerScore(keeper, GOALKEEPER, 6, 0));
    }

    @Test
    @DisplayName("Corrected ratings recalculate with the frozen table and publish again")
    void correctedRatingsRecalculateWithTheFrozenTableAndPublishAgain() {
        MatchScore match = withAllCopies();
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();
        Instant corrected = RATED.plus(Duration.ofHours(1));

        assertThat(match.applyRatings(Map.of(striker, 6), corrected)).isTrue();
        match.score(() -> {
            throw new AssertionError("The frozen table must be used");
        }, corrected);

        assertThat(match.ratingsAt()).isEqualTo(corrected);
        assertThat(match.scoredAt()).isEqualTo(corrected);
        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 18, false), new ManagerScore(brais, 12, false));
        assertThat(scoredEvent(match).players()).contains(new PlayerScoreData(striker, FORWARD, 6, 6, 12));
    }

    @Test
    @DisplayName("Older ratings delivered again after a correction are ignored")
    void olderRatingsDeliveredAgainAfterACorrectionAreIgnored() {
        MatchScore match = match();
        Instant corrected = RATED.plus(Duration.ofHours(1));
        match.applyRatings(Map.of(striker, 6), corrected);

        assertThat(match.applyRatings(UNRATED, RATED)).isFalse();
        assertThat(match.applyRatings(Map.of(striker, 6), corrected)).isFalse();

        assertThat(match.ratingPoints()).isEqualTo(Map.of(striker, 6));
        assertThat(match.ratingsAt()).isEqualTo(corrected);
    }

    @Test
    @DisplayName("Corrected ratings that change no points publish nothing")
    void correctedRatingsThatChangeNoPointsPublishNothing() {
        MatchScore match = withAllCopies();
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();

        // a player removed from the sheet had 0 for ratings: the rest are the same
        assertThat(match.applyRatings(Collections.singletonMap(keeper, 0), RATED.plusSeconds(60))).isTrue();
        match.score(table(DEFAULTS), RATED.plusSeconds(60));

        assertThat(match.scoredAt()).isEqualTo(NOW);
        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("A lineup over budget scores zero but its players still score")
    void aLineupOverBudgetScoresZeroButItsPlayersStillScore() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(List.of(new ManagerLineup(ana, List.of(keeper, striker), true)), LOCKED);
        match.applyRatings(UNRATED, RATED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.managerScores()).containsExactly(new ManagerScore(ana, 0, true));
        assertThat(match.playerScores()).extracting(PlayerScore::points).containsExactly(6, 6);
    }

    @Test
    @DisplayName("A lined-up player who did not play scores zero and an empty lineup scores zero")
    void aLinedUpPlayerWhoDidNotPlayScoresZeroAndAnEmptyLineupScoresZero() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(List.of(new ManagerLineup(ana, List.of(benched), false),
                new ManagerLineup(brais, List.of(), false)), LOCKED);
        match.applyRatings(UNRATED, RATED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.managerScores()).containsExactlyInAnyOrder(
                new ManagerScore(ana, 0, false), new ManagerScore(brais, 0, false));
        assertThat(match.playerScores()).extracting(PlayerScore::playerId).doesNotContain(benched);
    }

    @Test
    @DisplayName("Without lineups locked the match is scored with no managers")
    void withoutLineupsLockedTheMatchIsScoredWithNoManagers() {
        MatchScore match = match();
        match.applySheet(4, KICKOFF, sheet(), CLOSED);
        match.applyLineups(List.of(), LOCKED);
        match.applyRatings(UNRATED, RATED);

        match.score(table(DEFAULTS), NOW);

        assertThat(match.scored()).isTrue();
        assertThat(match.managerScores()).isEmpty();
        assertThat(scoredEvent(match).managers()).isEmpty();
    }

    @Test
    @DisplayName("Scoring the same data again changes nothing and publishes nothing")
    void scoringTheSameDataAgainChangesNothingAndPublishesNothing() {
        MatchScore match = withAllCopies();
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();

        assertThat(match.applySheet(4, KICKOFF, sheet(), CLOSED)).isFalse();
        assertThat(match.applyLineups(lineups(), LOCKED)).isFalse();
        assertThat(match.applyRatings(UNRATED, RATED)).isFalse();
        match.score(table(DEFAULTS), NOW.plusSeconds(60));

        assertThat(match.scoredAt()).isEqualTo(NOW);
        assertThat(match.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("A correction that changes points recalculates and publishes again")
    void aCorrectionThatChangesPointsRecalculatesAndPublishesAgain() {
        MatchScore match = withAllCopies();
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
    @DisplayName("A correction that only adds a player without points is published")
    void aCorrectionThatOnlyAddsAPlayerWithoutPointsIsPublished() {
        MatchScore match = withAllCopies();
        match.score(table(DEFAULTS), NOW);
        match.pullEvents();
        List<PlayerPerformance> withBenched = List.of(goals(keeper, GOALKEEPER, 1), goals(striker, FORWARD, 2),
                goals(benched, DEFENDER, 0));

        match.applySheet(4, KICKOFF, withBenched, NOW.plusSeconds(60));
        match.score(table(DEFAULTS), NOW.plusSeconds(60));

        assertThat(scoredEvent(match).players()).hasSize(3);
    }

    @Test
    @DisplayName("The recalculation uses the frozen table and does not read the current one")
    void theRecalculationUsesTheFrozenTableAndDoesNotReadTheCurrentOne() {
        ScoringTable tenPerGoal = TestScoringTables.withPoints(DEFAULTS, GOAL, 10);
        MatchScore match = withAllCopies();
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
    @DisplayName("An older sheet delivered again after a correction is ignored")
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
    @DisplayName("The sheet brings the matchday and kickoff of the closed match")
    void theSheetBringsTheMatchdayAndKickoffOfTheClosedMatch() {
        MatchScore lockedBeforeAPostponement = match();
        Instant postponed = KICKOFF.plus(Duration.ofMinutes(30));

        lockedBeforeAPostponement.applySheet(5, postponed, sheet(), CLOSED);

        assertThat(lockedBeforeAPostponement.matchday()).isEqualTo(5);
        assertThat(lockedBeforeAPostponement.kickoffAt()).isEqualTo(postponed);
    }

    @Test
    @DisplayName("An unlock drops the lineups locked before it and ignores them if delivered again")
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
        match.applyRatings(UNRATED, RATED);
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isFalse();

        Instant relocked = unlocked.plus(Duration.ofDays(7));
        assertThat(match.applyLineups(lineups(), relocked)).isTrue();
        match.score(table(DEFAULTS), NOW);
        assertThat(match.scored()).isTrue();
    }

    @Test
    @DisplayName("An older unlock delivered late keeps the newer lineups")
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
    @DisplayName("A scored match ignores an unlock and keeps its points")
    void aScoredMatchIgnoresAnUnlockAndKeepsItsPoints() {
        MatchScore match = withAllCopies();
        match.score(table(DEFAULTS), NOW);

        assertThat(match.unlock(NOW.plusSeconds(60))).isFalse();

        assertThat(match.lineups()).hasSize(2);
        assertThat(match.managerScores()).hasSize(2);
    }

    @Test
    @DisplayName("A player or a manager appears only once")
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
