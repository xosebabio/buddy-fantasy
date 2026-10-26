package com.buddyfantasy.league.adapter.in.web;

import com.buddyfantasy.league.application.usecases.getleaguesettings.LeagueSettingsView;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import lombok.experimental.UtilityClass;

@UtilityClass
class LeagueSettingsWebMapper {

    LeagueSettingsBody toBody(LeagueSettingsView view) {
        LeagueSettings s = view.settings();
        return new LeagueSettingsBody(s.ratingWindowHours(), s.ratingFormat(), s.ratingPointsPerUnit(),
                s.defaultRating(), s.lineupLockHours(), s.lineupMaxChanges(), s.lineupSize(),
                s.lineupFormationEnabled(), s.lineupBudgetEnabled(), s.lineupBudget(), s.marketK(),
                s.marketMaxChangePct(), s.marketMinValue(), view.scoringRules().points());
    }

    /** Throws {@link com.buddyfantasy.shared.domain.DomainValidationException} (400) on invalid values. */
    LeagueSettings toSettings(LeagueSettingsBody b) {
        return new LeagueSettings(b.ratingWindowHours(), b.ratingFormat(), b.ratingPointsPerUnit(),
                b.defaultRating(), b.lineupLockHours(), b.lineupMaxChanges(), b.lineupSize(),
                b.lineupFormationEnabled(), b.lineupBudgetEnabled(), b.lineupBudget(), b.marketK(),
                b.marketMaxChangePct(), b.marketMinValue());
    }

    ScoringRules toScoringRules(LeagueSettingsBody b) {
        return new ScoringRules(b.scoringRules());
    }
}
