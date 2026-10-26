package com.buddyfantasy.league.adapter.out.persistence;

import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import org.springframework.stereotype.Repository;

@Repository
class JpaLeagueRepository implements LeagueRepository {

    private final SpringDataLeagueRepository jpa;

    JpaLeagueRepository(SpringDataLeagueRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(League league) {
        jpa.save(LeagueMapper.toEntity(league));
    }
}
