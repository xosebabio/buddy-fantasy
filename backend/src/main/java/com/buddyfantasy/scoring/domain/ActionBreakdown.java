package com.buddyfantasy.scoring.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.ScoringAction;

/** A player's points per action in a match: only the actions that add or subtract, in the order of the enum. */
public record ActionBreakdown(Map<ScoringAction, Integer> points) {

    public ActionBreakdown {
        EnumMap<ScoringAction, Integer> copy = new EnumMap<>(ScoringAction.class);
        copy.putAll(points);
        copy.values().removeIf(value -> value == 0);
        points = Collections.unmodifiableMap(copy);
    }

    public int total() {
        return points.values().stream().mapToInt(Integer::intValue).sum();
    }
}
