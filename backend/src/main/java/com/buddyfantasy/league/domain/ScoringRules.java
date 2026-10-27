package com.buddyfantasy.league.domain;

import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Points per action and position. Always complete: every action has a value for every position. A new league is
 * seeded from {@code league.defaults.scoring-rules} (see {@code LeagueDefaultsRepository}).
 */
public record ScoringRules(Map<ScoringAction, Map<Position, Integer>> points) {

    public ScoringRules {
        EnumMap<ScoringAction, Map<Position, Integer>> copy = new EnumMap<>(ScoringAction.class);
        for (ScoringAction action : ScoringAction.values()) {
            Map<Position, Integer> byPosition = points == null ? null : points.get(action);
            if (byPosition == null) {
                throw new DomainValidationException("Missing scoring rules for " + action);
            }
            EnumMap<Position, Integer> positionCopy = new EnumMap<>(Position.class);
            for (Position position : Position.values()) {
                Integer value = byPosition.get(position);
                if (value == null) {
                    throw new DomainValidationException("Missing scoring rule for " + action + "/" + position);
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
}
