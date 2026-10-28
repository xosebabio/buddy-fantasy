package com.buddyfantasy.league.domain;

import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.adapter.out.config.TestLeagueDefaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.buddyfantasy.datadefinition.Position.*;
import static com.buddyfantasy.datadefinition.ScoringAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringRulesTests {

    private final ScoringRules defaults = TestLeagueDefaults.scoringRules();

    @Test
    @DisplayName("A goal's points depend on the position")
    void goalDependsOnPosition() {
        assertThat(defaults.pointsFor(GOAL, GOALKEEPER)).isEqualTo(6);
        assertThat(defaults.pointsFor(GOAL, DEFENDER)).isEqualTo(5);
        assertThat(defaults.pointsFor(GOAL, MIDFIELDER)).isEqualTo(4);
        assertThat(defaults.pointsFor(GOAL, FORWARD)).isEqualTo(3);
    }

    @Test
    @DisplayName("A penalty goal is worth +3 in every position")
    void penaltyGoalIsWorthThreeInEveryPosition() {
        for (Position position : Position.values()) {
            assertThat(defaults.pointsFor(PENALTY_GOAL, position)).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("A double yellow is worth -3 in total")
    void doubleYellowIsMinusThreeInTotal() {
        for (Position position : Position.values()) {
            assertThat(defaults.pointsFor(DOUBLE_YELLOW, position)).isEqualTo(-3);
        }
    }

    @Test
    @DisplayName("The position-independent actions match the requirements table")
    void positionIndependentActionsMatchRequirementsTable() {
        Map<ScoringAction, Integer> expected = Map.of(
                PENALTY_SAVED, 3, PENALTY_MISSED, -2, ASSIST, 1, OWN_GOAL, -3, YELLOW_CARD, -1, RED_CARD, -5);
        expected.forEach((action, points) -> {
            for (Position position : Position.values()) {
                assertThat(defaults.pointsFor(action, position)).as("%s/%s", action, position).isEqualTo(points);
            }
        });
    }

    @Test
    @DisplayName("The defaults cover all 36 action and position combinations")
    void defaultsCoverAll36Combinations() {
        int count = defaults.points().values().stream().mapToInt(Map::size).sum();
        assertThat(count).isEqualTo(ScoringAction.values().length * Position.values().length).isEqualTo(36);
    }

    @Test
    @DisplayName("Rejects an incomplete table")
    void rejectsIncompleteTable() {
        Map<ScoringAction, Map<Position, Integer>> incomplete = new EnumMap<>(defaults.points());
        incomplete.remove(RED_CARD);
        assertThatThrownBy(() -> new ScoringRules(incomplete)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejects a missing position")
    void rejectsMissingPosition() {
        Map<ScoringAction, Map<Position, Integer>> table = new EnumMap<>(defaults.points());
        table.put(GOAL, Map.of(GOALKEEPER, 6, DEFENDER, 5, MIDFIELDER, 4));
        assertThatThrownBy(() -> new ScoringRules(table)).isInstanceOf(IllegalArgumentException.class);
    }
}
