package com.buddyfantasy.league.adapter.in.web;

import java.math.BigDecimal;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.datadefinition.ScoringAction;

/**
 * Wire format of a league's parameters, used by GET and PUT. Money values are in millions. The rating conversion of
 * each format is {@code points = factor × average + base}; the league uses the one of {@code ratingFormat}.
 * They are stored configuration: consumer modules read them as they arrive.
 */
record LeagueSettingsBody(
        int ratingWindowHours,
        RatingFormat ratingFormat,
        BigDecimal ratingSpadesFactor,
        BigDecimal ratingSpadesBase,
        BigDecimal ratingDecimalFactor,
        BigDecimal ratingDecimalBase,
        int ratingMinVotes,
        BigDecimal defaultRating,
        int lineupLockHours,
        int lineupMaxChanges,
        int lineupSize,
        boolean lineupFormationEnabled,
        int lineupMinGoalkeepers,
        int lineupMinDefenders,
        int lineupMinMidfielders,
        int lineupMinForwards,
        boolean lineupBudgetEnabled,
        BigDecimal lineupBudget,
        BigDecimal marketK,
        BigDecimal marketMaxChangePct,
        BigDecimal marketMinValue,
        Map<ScoringAction, Map<Position, Integer>> scoringRules) {
}
