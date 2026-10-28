package com.buddyfantasy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.RecordMatchSheetCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Business limits come from configuration and the database does not repeat them: raising a limit above its old
 * column size or CHECK works without a migration, and every write is checked against the configured value.
 */
@SpringBootTest(properties = {
        "league.limits.name-max-length=150",
        "squad.limits.name-max-length=150",
        "squad.limits.shirt-number-max=150",
        "match.limits.opponent-max-length=150",
        "match.limits.goals-max=150",
        "match.limits.stat-count-max=150",
        "match.limits.sheet-players-max=1"})
@Import(PostgresTestConfig.class)
class ConfiguredLimitsTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Raised limits are accepted and stored, and are still enforced")
    void raisedLimitsAreAcceptedAndStoredAndAreStillEnforced() {
        UUID admin = UUID.randomUUID();
        UUID league = bus.dispatch(new CreateLeagueCommand(admin, "L".repeat(150))).id().value();
        assertThatThrownBy(() -> bus.dispatch(new CreateLeagueCommand(admin, "L".repeat(151))))
                .isInstanceOf(DomainValidationException.class);

        UUID player = bus.dispatch(new RegisterPlayerCommand(admin, league, "P".repeat(150), 150, Position.FORWARD,
                BigDecimal.TEN)).value();
        assertThatThrownBy(() -> bus.dispatch(new RegisterPlayerCommand(admin, league, "Iago", 151,
                Position.FORWARD, BigDecimal.TEN))).isInstanceOf(DomainValidationException.class);

        UUID match = bus.dispatch(new ScheduleMatchCommand(admin, league, 1, "R".repeat(150), Venue.HOME, KICKOFF))
                .value();
        bus.dispatch(new RecordMatchSheetCommand(admin, league, match, 150, 0,
                List.of(new PerformanceLine(player, 150, 0, 0, 0, 0, 0, false, false, false))));
        assertThatThrownBy(() -> bus.dispatch(new RecordMatchSheetCommand(admin, league, match, 1, 0, List.of(
                new PerformanceLine(player, 1, 0, 0, 0, 0, 0, false, false, false),
                new PerformanceLine(UUID.randomUUID(), 0, 0, 0, 0, 0, 0, false, false, false)))))
                .isInstanceOf(DomainValidationException.class);

        assertThat(jdbc.queryForObject("select length(name) from league_league where id = ?", Integer.class, league))
                .isEqualTo(150);
        assertThat(jdbc.queryForObject("select shirt_number from squad_player where id = ?", Integer.class, player))
                .isEqualTo(150);
        assertThat(jdbc.queryForObject("select goals from match_player_performance where match_id = ?",
                Integer.class, match)).isEqualTo(150);
    }
}
