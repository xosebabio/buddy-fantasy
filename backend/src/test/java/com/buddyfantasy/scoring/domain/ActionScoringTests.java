package com.buddyfantasy.scoring.domain;

import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.buddyfantasy.datadefinition.Position.*;
import static com.buddyfantasy.datadefinition.ScoringAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Points per action according to position (RF-15, RNF-11), with the default table and an edited one. */
class ActionScoringTests {

    static final ScoringTable DEFAULTS = TestScoringTables.defaults();

    static PlayerPerformance played(Position position) {
        return new PlayerPerformance(UUID.randomUUID(), position, 0, 0, 0, 0, 0, 0, false, false, false);
    }

    static PlayerPerformance goals(Position position, int goals) {
        return new PlayerPerformance(UUID.randomUUID(), position, goals, 0, 0, 0, 0, 0, false, false, false);
    }

    static PlayerPerformance cards(boolean yellow, boolean doubleYellow, boolean red) {
        return new PlayerPerformance(UUID.randomUUID(), DEFENDER, 0, 0, 0, 0, 0, 0, yellow, doubleYellow, red);
    }

    @Test
    @DisplayName("A goal is worth more the further back the player")
    void aGoalIsWorthMoreTheFurtherBackThePlayer() {
        assertThat(ActionScoring.breakdown(goals(GOALKEEPER, 1), DEFAULTS).total()).isEqualTo(6);
        assertThat(ActionScoring.breakdown(goals(DEFENDER, 1), DEFAULTS).total()).isEqualTo(5);
        assertThat(ActionScoring.breakdown(goals(MIDFIELDER, 1), DEFAULTS).total()).isEqualTo(4);
        assertThat(ActionScoring.breakdown(goals(FORWARD, 1), DEFAULTS).total()).isEqualTo(3);
    }

    @ParameterizedTest
    @DisplayName("A penalty goal is worth +3 in any position without adding the goal")
    @EnumSource(Position.class)
    void aPenaltyGoalIsWorthThreeInAnyPositionWithoutAddingTheGoal(Position position) {
        PlayerPerformance penalty = new PlayerPerformance(UUID.randomUUID(), position, 0, 1, 0, 0, 0, 0,
                false, false, false);

        ActionBreakdown breakdown = ActionScoring.breakdown(penalty, DEFAULTS);

        assertThat(breakdown.points()).containsExactly(Map.entry(PENALTY_GOAL, 3));
        assertThat(breakdown.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("A saved penalty adds 3 and a missed one takes 2")
    void aSavedPenaltyAddsThreeAndAMissedOneTakesTwo() {
        PlayerPerformance keeper = new PlayerPerformance(UUID.randomUUID(), GOALKEEPER, 0, 0, 1, 0, 0, 0,
                false, false, false);
        PlayerPerformance striker = new PlayerPerformance(UUID.randomUUID(), FORWARD, 0, 0, 0, 1, 0, 0,
                false, false, false);

        assertThat(ActionScoring.breakdown(keeper, DEFAULTS).points()).containsExactly(Map.entry(PENALTY_SAVED, 3));
        assertThat(ActionScoring.breakdown(striker, DEFAULTS).points()).containsExactly(Map.entry(PENALTY_MISSED, -2));
    }

    @Test
    @DisplayName("An own goal takes 3")
    void anOwnGoalTakesThree() {
        PlayerPerformance ownGoal = new PlayerPerformance(UUID.randomUUID(), DEFENDER, 0, 0, 0, 0, 0, 1,
                false, false, false);

        assertThat(ActionScoring.breakdown(ownGoal, DEFAULTS).total()).isEqualTo(-3);
    }

    @Test
    @DisplayName("A double yellow is -3 in total")
    void aDoubleYellowIsMinusThreeInTotal() {
        ActionBreakdown breakdown = ActionScoring.breakdown(cards(false, true, false), DEFAULTS);

        assertThat(breakdown.points()).containsExactly(Map.entry(DOUBLE_YELLOW, -3));
        assertThat(breakdown.total()).isEqualTo(-3);
    }

    @Test
    @DisplayName("A yellow and a straight red add up")
    void aYellowAndAStraightRedAddUp() {
        ActionBreakdown breakdown = ActionScoring.breakdown(cards(true, false, true), DEFAULTS);

        assertThat(breakdown.points()).containsExactly(Map.entry(YELLOW_CARD, -1), Map.entry(RED_CARD, -5));
        assertThat(breakdown.total()).isEqualTo(-6);
    }

    @Test
    @DisplayName("Several actions add up their count times their points")
    void severalActionsAddUpCountTimesTheirPoints() {
        PlayerPerformance midfielder = new PlayerPerformance(UUID.randomUUID(), MIDFIELDER, 2, 1, 0, 1, 3, 0,
                true, false, false);

        ActionBreakdown breakdown = ActionScoring.breakdown(midfielder, DEFAULTS);

        assertThat(breakdown.points()).containsExactly(Map.entry(GOAL, 8), Map.entry(PENALTY_GOAL, 3),
                Map.entry(PENALTY_MISSED, -2), Map.entry(ASSIST, 3), Map.entry(YELLOW_CARD, -1));
        assertThat(breakdown.total()).isEqualTo(11);
    }

    @Test
    @DisplayName("Playing without actions is zero with an empty breakdown")
    void playingWithoutActionsIsZeroWithAnEmptyBreakdown() {
        ActionBreakdown breakdown = ActionScoring.breakdown(played(GOALKEEPER), DEFAULTS);

        assertThat(breakdown.points()).isEmpty();
        assertThat(breakdown.total()).isZero();
    }

    @Test
    @DisplayName("Uses the table it is given and leaves out actions worth nothing")
    void usesTheTableItIsGivenAndLeavesOutActionsWorthNothing() {
        ScoringTable edited = TestScoringTables.withPoints(
                TestScoringTables.withPoints(DEFAULTS, GOAL, 10), ASSIST, 0);
        PlayerPerformance forward = new PlayerPerformance(UUID.randomUUID(), FORWARD, 1, 0, 0, 0, 2, 0,
                false, false, false);

        ActionBreakdown breakdown = ActionScoring.breakdown(forward, edited);

        assertThat(breakdown.points()).containsExactly(Map.entry(GOAL, 10));
        assertThat(breakdown.total()).isEqualTo(10);
    }

    @Test
    @DisplayName("A sheet line needs a player, a position and non-negative counts")
    void aSheetLineNeedsPlayerPositionAndNonNegativeCounts() {
        assertThatThrownBy(() -> new PlayerPerformance(null, FORWARD, 0, 0, 0, 0, 0, 0, false, false, false))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PlayerPerformance(UUID.randomUUID(), null, 0, 0, 0, 0, 0, 0, false, false, false))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> goals(FORWARD, -1)).isInstanceOf(DomainValidationException.class);
    }
}
