package com.buddyfantasy.league.adapter.in.web;

import com.buddyfantasy.league.application.usecases.getleaguesettings.LeagueSettingsView;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import lombok.experimental.UtilityClass;

@UtilityClass
class LeagueSettingsWebMapper {

    LeagueSettingsBody toBody(LeagueSettingsView view) {
        LeagueSettings s = view.settings();
        return new LeagueSettingsBody(s.ratingWindowHours(), s.ratingFormat(), s.ratingSpadesFactor(),
                s.ratingSpadesBase(), s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(),
                s.defaultRating(), s.lineupLockHours(), s.lineupMaxChanges(), s.lineupSize(),
                s.lineupFormationEnabled(), s.lineupMinGoalkeepers(), s.lineupMinDefenders(),
                s.lineupMinMidfielders(), s.lineupMinForwards(), s.lineupBudgetEnabled(), s.lineupBudget(),
                s.marketK(), s.marketMaxChangePct(), s.marketMinValue(), view.scoringRules().points());
    }

    /** Throws {@link com.buddyfantasy.shared.domain.DomainValidationException} (400) on invalid values. */
    LeagueSettings toSettings(LeagueSettingsBody b) {
        return new LeagueSettings(b.ratingWindowHours(), b.ratingFormat(), b.ratingSpadesFactor(),
                b.ratingSpadesBase(), b.ratingDecimalFactor(), b.ratingDecimalBase(), b.ratingMinVotes(),
                b.defaultRating(), b.lineupLockHours(), b.lineupMaxChanges(), b.lineupSize(),
                b.lineupFormationEnabled(), b.lineupMinGoalkeepers(), b.lineupMinDefenders(),
                b.lineupMinMidfielders(), b.lineupMinForwards(), b.lineupBudgetEnabled(), b.lineupBudget(),
                b.marketK(), b.marketMaxChangePct(), b.marketMinValue());
    }

    ScoringRules toScoringRules(LeagueSettingsBody b) {
        return new ScoringRules(b.scoringRules());
    }
}
