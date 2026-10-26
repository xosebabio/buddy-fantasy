package com.buddyfantasy.league.adapter.in.web;

import java.math.BigDecimal;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.domain.RatingFormat;

/**
 * Wire format of a league's parameters, used by GET and PUT. Money values are in millions.
 * They are stored configuration: consumer modules read them as they arrive.
 */
record LeagueSettingsBody(
        int ratingWindowHours,
        RatingFormat ratingFormat,
        BigDecimal ratingPointsPerUnit,
        BigDecimal defaultRating,
        int lineupLockHours,
        int lineupMaxChanges,
        int lineupSize,
        boolean lineupFormationEnabled,
        boolean lineupBudgetEnabled,
        BigDecimal lineupBudget,
        BigDecimal marketK,
        BigDecimal marketMaxChangePct,
        BigDecimal marketMinValue,
        Map<ScoringAction, Map<Position, Integer>> scoringRules) {
}
