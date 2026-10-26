package com.buddyfantasy.league.adapter.out.persistence;

import java.util.List;

import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueId;
import lombok.experimental.UtilityClass;

@UtilityClass
class LeagueMapper {

    League toDomain(LeagueEntity e, LeagueSettingsEntity settings, List<ScoringRuleEntity> rules) {
        return new League(new LeagueId(e.id), e.name, e.createdAt,
                LeagueSettingsMapper.toDomain(settings), ScoringRuleMapper.toDomain(rules));
    }

    LeagueEntity toEntity(League l) {
        return new LeagueEntity(l.id().value(), l.name(), l.createdAt());
    }
}
