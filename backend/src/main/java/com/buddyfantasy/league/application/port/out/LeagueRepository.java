package com.buddyfantasy.league.application.port.out;

import com.buddyfantasy.league.domain.League;

public interface LeagueRepository {

    void save(League league);
}
