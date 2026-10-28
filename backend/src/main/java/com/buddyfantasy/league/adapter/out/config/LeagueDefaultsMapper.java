package com.buddyfantasy.league.adapter.out.config;

import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.experimental.UtilityClass;

@UtilityClass
class LeagueDefaultsMapper {

    /** Throws {@link DomainValidationException} if a group is missing or a value is invalid. */
    LeagueSettings toSettings(LeagueDefaultsProperties p) {
        LeagueDefaultsProperties.Rating rating = require(p.rating(), "rating");
        LeagueDefaultsProperties.Lineup lineup = require(p.lineup(), "lineup");
        LeagueDefaultsProperties.Market market = require(p.market(), "market");
        LeagueDefaultsProperties.Conversions conversion = require(rating.conversion(), "rating.conversion");
        LeagueDefaultsProperties.Conversion spades = require(conversion.spades(), "rating.conversion.spades");
        LeagueDefaultsProperties.Conversion decimal = require(conversion.decimal(), "rating.conversion.decimal");
        return new LeagueSettings(rating.windowHours(), rating.format(), spades.factor(), spades.base(),
                decimal.factor(), decimal.base(), rating.minVotes(), rating.defaultRating(), lineup.lockHours(),
                lineup.maxChanges(), lineup.size(), lineup.formationEnabled(), lineup.minGoalkeepers(),
                lineup.minDefenders(), lineup.minMidfielders(), lineup.minForwards(), lineup.budgetEnabled(),
                lineup.budget(), market.k(), market.maxChangePct(), market.minValue());
    }

    /** Throws {@link DomainValidationException} if the table is incomplete. */
    ScoringRules toScoringRules(LeagueDefaultsProperties p) {
        return new ScoringRules(p.scoringRules());
    }

    private <T> T require(T group, String name) {
        if (group == null) {
            throw new DomainValidationException("Missing league.defaults." + name);
        }
        return group;
    }
}
