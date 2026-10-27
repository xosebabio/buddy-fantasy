package com.buddyfantasy.squad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.application.port.out.SquadLimitsRepository;
import com.buddyfantasy.squad.domain.Player;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"})
@Import(PostgresTestConfig.class)
class SquadApiTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID OTHER_LEAGUE = UUID.randomUUID();

    @Autowired
    SquadApi squad;

    @Autowired
    PlayerRepository players;

    @Autowired
    SquadLimitsRepository limits;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void reset() {
        jdbc.update("delete from squad_player");
    }

    private Player player(UUID leagueId, String name, Position position) {
        Player player = Player.register(leagueId, name, null, position, new BigDecimal("5"), NOW, limits.get());
        players.save(player);
        return player;
    }

    @Test
    void returnsTheRequestedPlayersOfTheLeagueWithPositionAndStatus() {
        Player keeper = player(LEAGUE, "Iago", Position.GOALKEEPER);
        Player forward = player(LEAGUE, "Brais", Position.FORWARD);
        player(LEAGUE, "Not asked", Position.DEFENDER);

        List<SquadPlayer> result = squad.findPlayers(LEAGUE, List.of(keeper.id().value(), forward.id().value()));

        assertThat(result).containsExactlyInAnyOrder(
                new SquadPlayer(keeper.id().value(), Position.GOALKEEPER, true),
                new SquadPlayer(forward.id().value(), Position.FORWARD, true));
    }

    @Test
    void retiredPlayersAreReturnedAsInactive() {
        Player retired = player(LEAGUE, "Iago", Position.MIDFIELDER);
        retired.retire(NOW);
        players.save(retired);

        assertThat(squad.findPlayers(LEAGUE, List.of(retired.id().value())))
                .containsExactly(new SquadPlayer(retired.id().value(), Position.MIDFIELDER, false));
    }

    @Test
    void playersOfAnotherLeagueAndUnknownIdsAreAbsent() {
        Player mine = player(LEAGUE, "Iago", Position.DEFENDER);
        Player foreign = player(OTHER_LEAGUE, "Brais", Position.DEFENDER);

        List<SquadPlayer> result = squad.findPlayers(LEAGUE,
                List.of(mine.id().value(), foreign.id().value(), UUID.randomUUID()));

        assertThat(result).extracting(SquadPlayer::id).containsExactly(mine.id().value());
    }

    @Test
    void repeatedIdsReturnThePlayerOnce() {
        Player player = player(LEAGUE, "Iago", Position.FORWARD);

        assertThat(squad.findPlayers(LEAGUE, List.of(player.id().value(), player.id().value()))).hasSize(1);
    }

    @Test
    void emptyRequestReturnsNothingWithoutQuerying() {
        Statistics statistics = statistics();

        assertThat(squad.findPlayers(LEAGUE, List.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    void loadsAllPlayersInASingleQuery() {
        List<UUID> ids = List.of(
                player(LEAGUE, "A", Position.GOALKEEPER).id().value(),
                player(LEAGUE, "B", Position.DEFENDER).id().value(),
                player(LEAGUE, "C", Position.MIDFIELDER).id().value(),
                player(LEAGUE, "D", Position.FORWARD).id().value());
        Statistics statistics = statistics();

        assertThat(squad.findPlayers(LEAGUE, ids)).hasSize(4);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
