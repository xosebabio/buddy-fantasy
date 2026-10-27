package com.buddyfantasy.match;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.match.application.port.out.MatchLimitsRepository;
import com.buddyfantasy.match.application.port.out.MatchRepository;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchSheet;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Matchday;
import com.buddyfantasy.match.domain.PlayerPerformance;
import com.buddyfantasy.match.domain.Venue;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** The whole aggregate, sheet included, round-trips through JPA; the sheet is replaced as a whole. */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"})
@Import(PostgresTestConfig.class)
class MatchRepositoryTests {

    static final UUID LEAGUE = UUID.randomUUID();
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant CREATED = KICKOFF.minus(Duration.ofDays(7));

    @Autowired
    MatchRepository matches;

    @Autowired
    MatchLimitsRepository limits;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void reset() {
        jdbc.update("delete from match_match");
    }

    static PlayerPerformance line(UUID playerId, Position position, int goals) {
        return new PlayerPerformance(playerId, position, goals, 1, 0, 1, 2, 0, true, false, true);
    }

    private Match scheduled() {
        Match match = Match.schedule(LEAGUE, new Matchday(3), "Os Ninguéns", Venue.AWAY, KICKOFF, CREATED,
                limits.get());
        matches.save(match);
        return match;
    }

    private Match load(Match match) {
        return matches.findByLeagueIdAndId(LEAGUE, match.id()).orElseThrow();
    }

    private int linesOf(Match match) {
        return jdbc.queryForObject("select count(*) from match_player_performance where match_id = ?",
                Integer.class, match.id().value());
    }

    @Test
    void savesAndLoadsTheWholeAggregate() {
        Match match = scheduled();
        MatchSheet sheet = new MatchSheet(new MatchResult(3, 1), List.of(
                line(UUID.randomUUID(), Position.FORWARD, 2),
                new PlayerPerformance(UUID.randomUUID(), Position.GOALKEEPER, 0, 0, 1, 0, 0, 1, false, true, false)));
        match.recordSheet(sheet, CREATED, limits.get());
        match.close(KICKOFF.plusSeconds(7200));
        matches.save(match);

        Match loaded = load(match);
        assertThat(loaded.leagueId()).isEqualTo(LEAGUE);
        assertThat(loaded.matchday()).isEqualTo(new Matchday(3));
        assertThat(loaded.opponent()).isEqualTo("Os Ninguéns");
        assertThat(loaded.venue()).isEqualTo(Venue.AWAY);
        assertThat(loaded.kickoffAt()).isEqualTo(KICKOFF);
        assertThat(loaded.status()).isEqualTo(MatchStatus.CLOSED);
        assertThat(loaded.createdAt()).isEqualTo(CREATED);
        assertThat(loaded.closedAt()).isEqualTo(KICKOFF.plusSeconds(7200));
        assertThat(loaded.sheet()).isEqualTo(sheet);
    }

    @Test
    void aScheduledMatchHasAnEmptySheet() {
        Match match = scheduled();

        assertThat(load(match).sheet()).isEqualTo(MatchSheet.EMPTY);
        assertThat(linesOf(match)).isZero();
    }

    @Test
    void replacingTheSheetInTheSameTransactionDropsTheOldLines() {
        Match match = scheduled();
        UUID kept = UUID.randomUUID();
        UUID dropped = UUID.randomUUID();
        match.recordSheet(new MatchSheet(new MatchResult(1, 0), List.of(
                line(kept, Position.FORWARD, 1), line(dropped, Position.DEFENDER, 0))), CREATED, limits.get());
        matches.save(match);

        // as a handler does: load and save the aggregate within one transaction
        MatchSheet corrected = new MatchSheet(new MatchResult(2, 0), List.of(
                line(kept, Position.FORWARD, 2), line(UUID.randomUUID(), Position.MIDFIELDER, 0)));
        transaction.executeWithoutResult(status -> {
            Match current = load(match);
            current.recordSheet(corrected, CREATED, limits.get());
            matches.save(current);
        });

        assertThat(load(match).sheet()).isEqualTo(corrected);
        assertThat(linesOf(match)).isEqualTo(2);
    }

    @Test
    void loadingIsScopedByLeague() {
        Match match = scheduled();

        assertThat(matches.findByLeagueIdAndId(UUID.randomUUID(), match.id())).isEmpty();
    }

    @Test
    void loadsAMatchWithItsSheetInASingleQuery() {
        Match match = scheduled();
        match.recordSheet(new MatchSheet(new MatchResult(1, 0), List.of(
                line(UUID.randomUUID(), Position.FORWARD, 1), line(UUID.randomUUID(), Position.DEFENDER, 0))),
                CREATED, limits.get());
        matches.save(match);
        Statistics statistics = statistics();

        assertThat(load(match).sheet().performances()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
