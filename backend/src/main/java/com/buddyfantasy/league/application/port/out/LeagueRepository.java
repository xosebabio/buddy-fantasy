package com.buddyfantasy.league.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;

public interface LeagueRepository {

    Optional<League> findById(UUID id);

    /** Settings of the given leagues in one query; unknown ids are absent. */
    Map<UUID, LeagueSettings> findSettingsByIds(Collection<UUID> ids);

    void save(League league);
}
