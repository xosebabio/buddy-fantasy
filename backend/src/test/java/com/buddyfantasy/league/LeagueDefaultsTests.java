package com.buddyfantasy.league;

import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** A new league is seeded from {@code league.defaults}, which can be overridden like any other property. */
@SpringBootTest(properties = {
        "league.defaults.lineup.size=9",
        "league.defaults.lineup.formation-enabled=true",
        "league.defaults.lineup.min-defenders=3",
        "league.defaults.scoring-rules.goal.forward=4",
        "league.defaults.scoring-rules.red-card.goalkeeper=-6"})
@Import(PostgresTestConfig.class)
class LeagueDefaultsTests {

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("A new league is seeded with the configured defaults")
    void newLeagueIsSeededWithTheConfiguredDefaults() {
        UUID league = bus.dispatch(new CreateLeagueCommand(UUID.randomUUID(), "Peña")).id().value();

        assertThat(jdbc.queryForObject("select lineup_size from league_settings where league_id = ?",
                Integer.class, league)).isEqualTo(9);
        assertThat(jdbc.queryForObject("select lineup_formation_enabled from league_settings where league_id = ?",
                Boolean.class, league)).isTrue();
        assertThat(jdbc.queryForObject("select lineup_min_defenders from league_settings where league_id = ?",
                Integer.class, league)).isEqualTo(3);
        assertThat(points(league, "GOAL", "FORWARD")).isEqualTo(4);
        assertThat(points(league, "RED_CARD", "GOALKEEPER")).isEqualTo(-6);
        assertThat(points(league, "GOAL", "GOALKEEPER")).isEqualTo(6);
    }

    private int points(UUID league, String action, String position) {
        return jdbc.queryForObject("""
                select points from league_scoring_rule where league_id = ? and action = ? and position = ?""",
                Integer.class, league, action, position);
    }
}
