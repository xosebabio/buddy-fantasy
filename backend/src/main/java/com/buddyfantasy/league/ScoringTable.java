package com.buddyfantasy.league;

import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;

/**
 * Points per action and position of a league (RF-15), as seen by other modules. Complete: every action has a value
 * for every position.
 */
public record ScoringTable(Map<ScoringAction, Map<Position, Integer>> points) {

    public ScoringTable {
        EnumMap<ScoringAction, Map<Position, Integer>> copy = new EnumMap<>(ScoringAction.class);
        points.forEach((action, byPosition) -> copy.put(action, Map.copyOf(new EnumMap<>(byPosition))));
        points = Map.copyOf(copy);
    }

    public int pointsFor(ScoringAction action, Position position) {
        return points.get(action).get(position);
    }
}
