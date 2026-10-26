package com.buddyfantasy.league.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.usecases.listmyleagues.MyLeagueView;
import com.buddyfantasy.league.application.usecases.listmyleagues.MyLeaguesReadModel;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMyLeaguesReadModel implements MyLeaguesReadModel {

    private final JdbcClient jdbc;

    JdbcMyLeaguesReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MyLeagueView> findByUserId(UUID userId) {
        return jdbc.sql("""
                        select l.id, l.name, m.role
                        from league_membership m
                        join league_league l on l.id = m.league_id
                        where m.user_id = :userId
                        order by l.name""")
                .param("userId", userId)
                .query((rs, i) -> new MyLeagueView(
                        rs.getObject("id", UUID.class), rs.getString("name"), LeagueRole.valueOf(rs.getString("role"))))
                .list();
    }
}
