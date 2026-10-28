package com.buddyfantasy.scoring.domain;

import java.util.EnumMap;

import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.ScoringTable;
import lombok.experimental.UtilityClass;

/**
 * Points per action of a sheet line (RF-15): Σ count × the table's points for (action, position of the sheet). A
 * penalty goal is not also a goal and a double yellow is not also a yellow card in the sheet, so a penalty goal is
 * worth its own points in any position and a double yellow its own in total, with no special case here.
 */
@UtilityClass
public class ActionScoring {

    public ActionBreakdown breakdown(PlayerPerformance performance, ScoringTable table) {
        EnumMap<ScoringAction, Integer> points = new EnumMap<>(ScoringAction.class);
        for (ScoringAction action : ScoringAction.values()) {
            points.put(action, count(performance, action) * table.pointsFor(action, performance.position()));
        }
        return new ActionBreakdown(points);
    }

    /** How many times the player did {@code action}: the fixed link between actions and the stats of the sheet. */
    int count(PlayerPerformance performance, ScoringAction action) {
        return switch (action) {
            case GOAL -> performance.goals();
            case PENALTY_GOAL -> performance.penaltyGoals();
            case PENALTY_SAVED -> performance.penaltiesSaved();
            case PENALTY_MISSED -> performance.penaltiesMissed();
            case ASSIST -> performance.assists();
            case OWN_GOAL -> performance.ownGoals();
            case YELLOW_CARD -> performance.yellowCard() ? 1 : 0;
            case DOUBLE_YELLOW -> performance.doubleYellow() ? 1 : 0;
            case RED_CARD -> performance.redCard() ? 1 : 0;
        };
    }
}
