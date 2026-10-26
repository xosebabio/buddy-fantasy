package com.buddyfantasy.league.adapter.out.persistence;

import java.util.UUID;

import com.buddyfantasy.league.domain.LeagueSettings;
import lombok.experimental.UtilityClass;

@UtilityClass
class LeagueSettingsMapper {

    LeagueSettings toDomain(LeagueSettingsEntity e) {
        return new LeagueSettings(e.ratingWindowHours, e.ratingFormat, e.ratingPointsPerUnit, e.defaultRating,
                e.lineupLockHours, e.lineupMaxChanges, e.lineupSize, e.lineupFormationEnabled,
                e.lineupBudgetEnabled, e.lineupBudget, e.marketK, e.marketMaxChangePct, e.marketMinValue);
    }

    LeagueSettingsEntity toEntity(UUID leagueId, LeagueSettings s) {
        return new LeagueSettingsEntity(leagueId, s.ratingWindowHours(), s.ratingFormat(), s.ratingPointsPerUnit(),
                s.defaultRating(), s.lineupLockHours(), s.lineupMaxChanges(), s.lineupSize(),
                s.lineupFormationEnabled(), s.lineupBudgetEnabled(), s.lineupBudget(), s.marketK(),
                s.marketMaxChangePct(), s.marketMinValue());
    }
}
