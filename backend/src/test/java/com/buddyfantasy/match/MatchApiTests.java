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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"})
@Import(PostgresTestConfig.class)
class MatchApiTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID OTHER_LEAGUE = UUID.randomUUID();

    @Autowired
    MatchApi api;

    @Autowired
    MatchRepository matches;

    @Autowired
    MatchLimitsRepository limits;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void reset() {
        jdbc.update("delete from match_match");
    }

    private Match scheduled(UUID leagueId, int matchday, Instant kickoffAt) {
        Match match = Match.schedule(leagueId, new Matchday(matchday), "Rival " + matchday, Venue.HOME, kickoffAt,
                kickoffAt.minus(Duration.ofDays(7)), limits.get());
        matches.save(match);
        return match;
    }

    private Match closed(UUID leagueId, int matchday, Instant kickoffAt) {
        Match match = scheduled(leagueId, matchday, kickoffAt);
        match.recordSheet(new MatchSheet(new MatchResult(1, 0), List.of(
                new PlayerPerformance(UUID.randomUUID(), Position.FORWARD, 1, 0, 0, 0, 0, 0, false, false, false))),
                kickoffAt, limits.get());
        match.close(kickoffAt.plus(Duration.ofHours(2)));
        matches.save(match);
        return match;
    }

    private static MatchSchedule schedule(Match match, boolean closed) {
        return new MatchSchedule(match.id().value(), match.leagueId(), match.matchday().value(), match.kickoffAt(),
                closed);
    }

    @Test
    void findMatchesReturnsTheLeagueCalendarOrderedByKickoffNotByMatchday() {
        Match first = closed(LEAGUE, 1, KICKOFF);
        // matchday 2 was postponed after matchday 3
        Match postponed = scheduled(LEAGUE, 2, KICKOFF.plus(Duration.ofDays(14)));
        Match third = scheduled(LEAGUE, 3, KICKOFF.plus(Duration.ofDays(7)));
        scheduled(OTHER_LEAGUE, 1, KICKOFF.plus(Duration.ofDays(1)));

        assertThat(api.findMatches(LEAGUE)).containsExactly(
                schedule(first, true), schedule(third, false), schedule(postponed, false));
    }

    @Test
    void findMatchesOfALeagueWithoutMatchesIsEmpty() {
        scheduled(OTHER_LEAGUE, 1, KICKOFF);

        assertThat(api.findMatches(LEAGUE)).isEmpty();
    }

    @Test
    void findScheduledKickingOffBeforeReturnsScheduledMatchesOfEveryLeagueBeforeTheInstant() {
        Instant until = KICKOFF.plus(Duration.ofDays(7));
        closed(LEAGUE, 1, KICKOFF.minus(Duration.ofDays(7)));
        Match pastNotClosed = scheduled(LEAGUE, 2, KICKOFF);
        Match otherLeague = scheduled(OTHER_LEAGUE, 1, KICKOFF.plus(Duration.ofDays(3)));
        scheduled(LEAGUE, 3, until);
        scheduled(LEAGUE, 4, until.plus(Duration.ofDays(7)));

        assertThat(api.findScheduledKickingOffBefore(until)).containsExactly(
                schedule(pastNotClosed, false), schedule(otherLeague, false));
    }

    @Test
    void eachMethodIsASingleQueryWithoutLoadingTheSheets() {
        closed(LEAGUE, 1, KICKOFF);
        closed(LEAGUE, 2, KICKOFF.plus(Duration.ofDays(7)));
        scheduled(LEAGUE, 3, KICKOFF.plus(Duration.ofDays(14)));
        Statistics statistics = statistics();

        assertThat(api.findMatches(LEAGUE)).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);

        statistics.clear();
        assertThat(api.findScheduledKickingOffBefore(KICKOFF.plus(Duration.ofDays(30)))).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
