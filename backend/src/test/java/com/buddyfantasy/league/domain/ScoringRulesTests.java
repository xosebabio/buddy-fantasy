package com.buddyfantasy.league.domain;

import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import org.junit.jupiter.api.Test;

import static com.buddyfantasy.datadefinition.Position.*;
import static com.buddyfantasy.datadefinition.ScoringAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringRulesTests {

    private final ScoringRules defaults = ScoringRules.defaults();

    @Test
    void goalDependsOnPosition() {
        assertThat(defaults.pointsFor(GOAL, GOALKEEPER)).isEqualTo(6);
        assertThat(defaults.pointsFor(GOAL, DEFENDER)).isEqualTo(5);
        assertThat(defaults.pointsFor(GOAL, MIDFIELDER)).isEqualTo(4);
        assertThat(defaults.pointsFor(GOAL, FORWARD)).isEqualTo(3);
    }

    @Test
    void penaltyGoalIsWorthThreeInEveryPosition() {
        for (Position position : Position.values()) {
            assertThat(defaults.pointsFor(PENALTY_GOAL, position)).isEqualTo(3);
        }
    }

    @Test
    void doubleYellowIsMinusThreeInTotal() {
        for (Position position : Position.values()) {
            assertThat(defaults.pointsFor(DOUBLE_YELLOW, position)).isEqualTo(-3);
        }
    }

    @Test
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
    void defaultsCoverAll36Combinations() {
        int count = defaults.points().values().stream().mapToInt(Map::size).sum();
        assertThat(count).isEqualTo(ScoringAction.values().length * Position.values().length).isEqualTo(36);
    }

    @Test
    void rejectsIncompleteTable() {
        Map<ScoringAction, Map<Position, Integer>> incomplete = new EnumMap<>(defaults.points());
        incomplete.remove(RED_CARD);
        assertThatThrownBy(() -> new ScoringRules(incomplete)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingPosition() {
        Map<ScoringAction, Map<Position, Integer>> table = new EnumMap<>(defaults.points());
        table.put(GOAL, Map.of(GOALKEEPER, 6, DEFENDER, 5, MIDFIELDER, 4));
        assertThatThrownBy(() -> new ScoringRules(table)).isInstanceOf(IllegalArgumentException.class);
    }
}
