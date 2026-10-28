package com.buddyfantasy.scoring.domain;

import java.util.EnumMap;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.league.adapter.out.config.TestLeagueDefaults;
import lombok.experimental.UtilityClass;

/** Tables of points for unit tests: the one in the real {@code application.yml}, and edited copies of it. */
@UtilityClass
class TestScoringTables {

    ScoringTable defaults() {
        return new ScoringTable(TestLeagueDefaults.scoringRules().points());
    }

    /** {@code table} with {@code action} worth {@code points} in every position. */
    ScoringTable withPoints(ScoringTable table, ScoringAction action, int points) {
        Map<ScoringAction, Map<Position, Integer>> edited = new EnumMap<>(table.points());
        Map<Position, Integer> byPosition = new EnumMap<>(Position.class);
        for (Position position : Position.values()) {
            byPosition.put(position, points);
        }
        edited.put(action, byPosition);
        return new ScoringTable(edited);
    }
}
