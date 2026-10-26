package com.buddyfantasy.league.domain;

import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;

/** Points per action and position. Always complete: every action has a value for every position. */
public record ScoringRules(Map<ScoringAction, Map<Position, Integer>> points) {

    public ScoringRules {
        EnumMap<ScoringAction, Map<Position, Integer>> copy = new EnumMap<>(ScoringAction.class);
        for (ScoringAction action : ScoringAction.values()) {
            Map<Position, Integer> byPosition = points == null ? null : points.get(action);
            if (byPosition == null) {
                throw new IllegalArgumentException("Missing scoring rules for " + action);
            }
            EnumMap<Position, Integer> positionCopy = new EnumMap<>(Position.class);
            for (Position position : Position.values()) {
                Integer value = byPosition.get(position);
                if (value == null) {
                    throw new IllegalArgumentException("Missing scoring rule for " + action + "/" + position);
                }
                positionCopy.put(position, value);
            }
            copy.put(action, Map.copyOf(positionCopy));
        }
        points = Map.copyOf(copy);
    }

    public int pointsFor(ScoringAction action, Position position) {
        return points.get(action).get(position);
    }

    public static ScoringRules defaults() {
        EnumMap<ScoringAction, Map<Position, Integer>> table = new EnumMap<>(ScoringAction.class);
        table.put(ScoringAction.GOAL, byPosition(6, 5, 4, 3));
        table.put(ScoringAction.PENALTY_GOAL, uniform(3));
        table.put(ScoringAction.PENALTY_SAVED, uniform(3));
        table.put(ScoringAction.PENALTY_MISSED, uniform(-2));
        table.put(ScoringAction.ASSIST, uniform(1));
        table.put(ScoringAction.OWN_GOAL, uniform(-3));
        table.put(ScoringAction.YELLOW_CARD, uniform(-1));
        table.put(ScoringAction.DOUBLE_YELLOW, uniform(-3));
        table.put(ScoringAction.RED_CARD, uniform(-5));
        return new ScoringRules(table);
    }

    private static Map<Position, Integer> byPosition(int goalkeeper, int defender, int midfielder, int forward) {
        return Map.of(Position.GOALKEEPER, goalkeeper, Position.DEFENDER, defender,
                Position.MIDFIELDER, midfielder, Position.FORWARD, forward);
    }

    private static Map<Position, Integer> uniform(int value) {
        return byPosition(value, value, value, value);
    }
}
