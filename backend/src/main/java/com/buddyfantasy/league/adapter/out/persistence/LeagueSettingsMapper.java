package com.buddyfantasy.league.adapter.out.persistence;

import java.util.UUID;

import com.buddyfantasy.league.domain.LeagueSettings;
import lombok.experimental.UtilityClass;

@UtilityClass
class LeagueSettingsMapper {

    LeagueSettings toDomain(LeagueSettingsEntity e) {
        return new LeagueSettings(e.ratingWindowHours, e.ratingFormat, e.ratingSpadesFactor, e.ratingSpadesBase,
                e.ratingDecimalFactor, e.ratingDecimalBase, e.ratingMinVotes, e.defaultRating, e.lineupLockHours,
                e.lineupMaxChanges, e.lineupSize, e.lineupFormationEnabled, e.lineupMinGoalkeepers,
                e.lineupMinDefenders, e.lineupMinMidfielders, e.lineupMinForwards, e.lineupBudgetEnabled,
                e.lineupBudget, e.marketK, e.marketMaxChangePct, e.marketMinValue);
    }

    LeagueSettingsEntity toEntity(UUID leagueId, LeagueSettings s) {
        return new LeagueSettingsEntity(leagueId, s.ratingWindowHours(), s.ratingFormat(), s.ratingSpadesFactor(),
                s.ratingSpadesBase(), s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(),
                s.defaultRating(), s.lineupLockHours(), s.lineupMaxChanges(), s.lineupSize(),
                s.lineupFormationEnabled(), (short) s.lineupMinGoalkeepers(), (short) s.lineupMinDefenders(),
                (short) s.lineupMinMidfielders(), (short) s.lineupMinForwards(), s.lineupBudgetEnabled(),
                s.lineupBudget(), s.marketK(), s.marketMaxChangePct(), s.marketMinValue());
    }
}
