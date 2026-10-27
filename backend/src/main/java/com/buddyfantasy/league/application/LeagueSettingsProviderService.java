package com.buddyfantasy.league.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;

@Service
class LeagueSettingsProviderService implements LeagueSettingsProvider {

    private final LeagueRepository leagues;

    LeagueSettingsProviderService(LeagueRepository leagues) {
        this.leagues = leagues;
    }

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

    private static LineupSettings toLineupSettings(LeagueSettings s) {
        return new LineupSettings(s.lineupSize(), s.lineupLockHours(), s.lineupMaxChanges(),
                s.lineupFormationEnabled(), s.lineupMinGoalkeepers(), s.lineupMinDefenders(),
                s.lineupMinMidfielders(), s.lineupMinForwards(), s.lineupBudgetEnabled(), s.lineupBudget());
    }
}
