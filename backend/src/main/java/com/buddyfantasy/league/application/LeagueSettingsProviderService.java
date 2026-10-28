package com.buddyfantasy.league.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.league.RatingSettings;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class LeagueSettingsProviderService implements LeagueSettingsProvider {

    private final LeagueRepository leagues;
    private final LeagueLimitsRepository limits;

    @Override
    public LineupSettings lineupSettings(UUID leagueId) {
        LineupSettings settings = lineupSettings(List.of(leagueId)).get(leagueId);
        if (settings == null) {
            throw new NotFoundException("League " + leagueId + " not found");
        }
        return settings;
    }

    @Override
    public Map<UUID, LineupSettings> lineupSettings(Collection<UUID> leagueIds) {
        if (leagueIds.isEmpty()) {
            return Map.of();
        }
        return leagues.findSettingsByIds(leagueIds).entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> toLineupSettings(e.getValue())));
    }

    @Override
    public ScoringTable scoringRules(UUID leagueId) {
        return leagues.findScoringRules(leagueId)
                .map(rules -> new ScoringTable(rules.points()))
                .orElseThrow(() -> new NotFoundException("League " + leagueId + " not found"));
    }

    @Override
    public RatingSettings ratingSettings(UUID leagueId) {
        LeagueSettings s = leagues.findSettingsByIds(List.of(leagueId)).get(leagueId);
        if (s == null) {
            throw new NotFoundException("League " + leagueId + " not found");
        }
        return new RatingSettings(s.ratingWindowHours(), s.ratingFormat(), s.ratingFactor(), s.ratingBase(),
                s.ratingMinVotes(), s.defaultRating());
    }

    @Override
    public int lineupLockHoursMax() {
        return limits.get().lineupLockHoursMax();
    }

    private static LineupSettings toLineupSettings(LeagueSettings s) {
        return new LineupSettings(s.lineupSize(), s.lineupLockHours(), s.lineupMaxChanges(),
                s.lineupFormationEnabled(), s.lineupMinGoalkeepers(), s.lineupMinDefenders(),
                s.lineupMinMidfielders(), s.lineupMinForwards(), s.lineupBudgetEnabled(), s.lineupBudget());
    }
}
