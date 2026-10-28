package com.buddyfantasy.league.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;

public interface LeagueRepository {

    Optional<League> findById(UUID id);

    /** Settings of the given leagues in one query; unknown ids are absent. */
    Map<UUID, LeagueSettings> findSettingsByIds(Collection<UUID> ids);

    /** Scoring rules of the league in one query; empty if the league does not exist. */
    Optional<ScoringRules> findScoringRules(UUID id);

    void save(League league);
}
