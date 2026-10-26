package com.buddyfantasy.league.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.domain.League;

public interface LeagueRepository {

    Optional<League> findById(UUID id);

    void save(League league);
}
