package com.buddyfantasy.league.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.RatingFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "league_settings")
class LeagueSettingsEntity {

    @Id
    @Column(name = "league_id")
    UUID leagueId;

    @Column(name = "rating_window_hours", nullable = false)
    int ratingWindowHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating_format", nullable = false)
    RatingFormat ratingFormat;

    @Column(name = "rating_spades_factor", nullable = false)
    BigDecimal ratingSpadesFactor;

    @Column(name = "rating_spades_base", nullable = false)
    BigDecimal ratingSpadesBase;

    @Column(name = "rating_decimal_factor", nullable = false)
    BigDecimal ratingDecimalFactor;

    @Column(name = "rating_decimal_base", nullable = false)
    BigDecimal ratingDecimalBase;

    @Column(name = "rating_min_votes", nullable = false)
    int ratingMinVotes;

    @Column(name = "default_rating")
    BigDecimal defaultRating;

    @Column(name = "lineup_lock_hours", nullable = false)
    int lineupLockHours;

    @Column(name = "lineup_max_changes", nullable = false)
    int lineupMaxChanges;

    @Column(name = "lineup_size", nullable = false)
    int lineupSize;

    @Column(name = "lineup_formation_enabled", nullable = false)
    boolean lineupFormationEnabled;

    @Column(name = "lineup_min_goalkeepers", nullable = false)
    short lineupMinGoalkeepers;

    @Column(name = "lineup_min_defenders", nullable = false)
    short lineupMinDefenders;

    @Column(name = "lineup_min_midfielders", nullable = false)
    short lineupMinMidfielders;

    @Column(name = "lineup_min_forwards", nullable = false)
    short lineupMinForwards;

    @Column(name = "lineup_budget_enabled", nullable = false)
    boolean lineupBudgetEnabled;

    @Column(name = "lineup_budget", nullable = false)
    BigDecimal lineupBudget;

    @Column(name = "market_k", nullable = false)
    BigDecimal marketK;

    @Column(name = "market_max_change_pct", nullable = false)
    BigDecimal marketMaxChangePct;

    @Column(name = "market_min_value", nullable = false)
    BigDecimal marketMinValue;

    protected LeagueSettingsEntity() {
    }

    LeagueSettingsEntity(UUID leagueId, int ratingWindowHours, RatingFormat ratingFormat,
                         BigDecimal ratingSpadesFactor, BigDecimal ratingSpadesBase, BigDecimal ratingDecimalFactor,
                         BigDecimal ratingDecimalBase, int ratingMinVotes, BigDecimal defaultRating, int lineupLockHours,
                         int lineupMaxChanges, int lineupSize, boolean lineupFormationEnabled,
                         short lineupMinGoalkeepers, short lineupMinDefenders, short lineupMinMidfielders,
                         short lineupMinForwards, boolean lineupBudgetEnabled, BigDecimal lineupBudget,
                         BigDecimal marketK, BigDecimal marketMaxChangePct, BigDecimal marketMinValue) {
        this.leagueId = leagueId;
        this.ratingWindowHours = ratingWindowHours;
        this.ratingFormat = ratingFormat;
        this.ratingSpadesFactor = ratingSpadesFactor;
        this.ratingSpadesBase = ratingSpadesBase;
        this.ratingDecimalFactor = ratingDecimalFactor;
        this.ratingDecimalBase = ratingDecimalBase;
        this.ratingMinVotes = ratingMinVotes;
        this.defaultRating = defaultRating;
        this.lineupLockHours = lineupLockHours;
        this.lineupMaxChanges = lineupMaxChanges;
        this.lineupSize = lineupSize;
        this.lineupFormationEnabled = lineupFormationEnabled;
        this.lineupMinGoalkeepers = lineupMinGoalkeepers;
        this.lineupMinDefenders = lineupMinDefenders;
        this.lineupMinMidfielders = lineupMinMidfielders;
        this.lineupMinForwards = lineupMinForwards;
        this.lineupBudgetEnabled = lineupBudgetEnabled;
        this.lineupBudget = lineupBudget;
        this.marketK = marketK;
        this.marketMaxChangePct = marketMaxChangePct;
        this.marketMinValue = marketMinValue;
    }
}
