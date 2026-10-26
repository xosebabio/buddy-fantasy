package com.buddyfantasy.league.adapter.out.persistence;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.domain.ScoringRules;
import lombok.experimental.UtilityClass;

@UtilityClass
class ScoringRuleMapper {

    ScoringRules toDomain(List<ScoringRuleEntity> entities) {
        Map<ScoringAction, Map<Position, Integer>> table = new EnumMap<>(ScoringAction.class);
        for (ScoringRuleEntity e : entities) {
            table.computeIfAbsent(e.action, action -> new EnumMap<>(Position.class)).put(e.position, e.points);
        }
        return new ScoringRules(table);
    }

    List<ScoringRuleEntity> toEntities(UUID leagueId, ScoringRules rules) {
        List<ScoringRuleEntity> entities = new ArrayList<>();
        rules.points().forEach((action, byPosition) -> byPosition.forEach(
                (position, points) -> entities.add(new ScoringRuleEntity(leagueId, action, position, points))));
        return entities;
    }
}
