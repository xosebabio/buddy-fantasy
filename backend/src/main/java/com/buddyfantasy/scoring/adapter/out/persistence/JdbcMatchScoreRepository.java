package com.buddyfantasy.scoring.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.ManagerLineup;
import com.buddyfantasy.scoring.domain.ManagerScore;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.scoring.domain.MatchdayPoints;
import com.buddyfantasy.scoring.domain.PlayerPerformance;
import com.buddyfantasy.scoring.domain.PlayerScore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * A match is a small aggregate (~15 sheet lines, ~20 lineups of ~7 players), so saving it rewrites its copies; no JPA
 * entity. The frozen table is stored as JSONB with the application's {@link ObjectMapper}, as points per action and
 * position.
 */
@Repository
class JdbcMatchScoreRepository implements MatchScoreRepository {

    private static final TypeReference<Map<ScoringAction, Map<Position, Integer>>> RULES = new TypeReference<>() {
    };
    private static final String SELECT_MATCH = """
            select match_id, league_id, matchday, kickoff_at, sheet_at, lineups_at, unlocked_at, rules, scored_at
            from scoring_match
            where match_id = :matchId""";

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;
    private final ObjectMapper json;

    JdbcMatchScoreRepository(JdbcClient jdbc, JdbcTemplate batch, ObjectMapper json) {
        this.jdbc = jdbc;
        this.batch = batch;
        this.json = json;
    }

    @Override
    public MatchScore lockOrCreate(MatchScore initial) {
        // a concurrent insert of the same match waits for the other transaction and then does nothing
        jdbc.sql("""
                        insert into scoring_match (match_id, league_id, matchday, kickoff_at)
                        values (:matchId, :leagueId, :matchday, :kickoffAt)
                        on conflict (match_id) do nothing""")
                .param("matchId", initial.matchId())
                .param("leagueId", initial.leagueId())
                .param("matchday", initial.matchday())
                .param("kickoffAt", utc(initial.kickoffAt()))
                .update();
        return lock(initial.matchId()).orElseThrow();
    }

    @Override
    public Optional<MatchScore> lock(UUID matchId) {
        return find(SELECT_MATCH + " for update", matchId);
    }

    @Override
    public Optional<MatchScore> findById(UUID matchId) {
        return find(SELECT_MATCH, matchId);
    }

    private Optional<MatchScore> find(String sql, UUID matchId) {
        Optional<MatchRow> match = jdbc.sql(sql)
                .param("matchId", matchId)
                .query(this::toMatchRow)
                .optional();
        return match.map(this::load);
    }

    /** The copies are read after the lock, so they include whatever the transaction that held it committed. */
    private MatchScore load(MatchRow match) {
        List<PlayerRow> players = jdbc.sql("""
                        select player_id, position, goals, penalty_goals, penalties_saved, penalties_missed, assists,
                               own_goals, yellow_card, double_yellow, red_card, action_points, rating_points
                        from scoring_player_score
                        where match_id = :matchId
                        order by player_id""")
                .param("matchId", match.matchId())
                .query(JdbcMatchScoreRepository::toPlayerRow)
                .list();
        List<LineupRow> lineupRows = jdbc.sql("""
                        select l.user_id, l.over_budget, l.points, p.player_id
                        from scoring_lineup l
                        left join scoring_lineup_player p on p.match_id = l.match_id and p.user_id = l.user_id
                        where l.match_id = :matchId
                        order by l.user_id, p.player_id""")
                .param("matchId", match.matchId())
                .query(JdbcMatchScoreRepository::toLineupRow)
                .list();
        Map<UUID, LineupRow> lineups = new LinkedHashMap<>();
        Map<UUID, List<UUID>> lineupPlayers = new LinkedHashMap<>();
        for (LineupRow row : lineupRows) {
            lineups.putIfAbsent(row.userId(), row);
            List<UUID> playerIds = lineupPlayers.computeIfAbsent(row.userId(), id -> new ArrayList<>());
            if (row.playerId() != null) {
                playerIds.add(row.playerId());
            }
        }
        boolean scored = match.scoredAt() != null;
        return new MatchScore(match.matchId(), match.leagueId(), match.matchday(), match.kickoffAt(),
                players.stream().map(PlayerRow::performance).toList(), match.sheetAt(),
                lineups.values().stream()
                        .map(row -> new ManagerLineup(row.userId(), lineupPlayers.get(row.userId()), row.overBudget()))
                        .toList(),
                match.lineupsAt(), match.unlockedAt(), match.rules(), match.scoredAt(),
                scored ? players.stream().map(PlayerRow::score).toList() : List.of(),
                scored ? lineups.values().stream()
                        .map(row -> new ManagerScore(row.userId(), row.points(), row.overBudget()))
                        .toList() : List.of());
    }

    /** A scored match without lineups has a single row with a null user. */
    @Override
    public List<MatchdayPoints> findScoredMatchdayPoints(UUID leagueId) {
        List<ManagerPointsRow> rows = jdbc.sql("""
                        select m.match_id, m.kickoff_at, l.user_id, l.points
                        from scoring_match m
                        left join scoring_lineup l on l.match_id = m.match_id
                        where m.league_id = :leagueId and m.scored_at is not null
                        order by m.kickoff_at, m.match_id""")
                .param("leagueId", leagueId)
                .query((rs, rowNum) -> new ManagerPointsRow(rs.getObject("match_id", UUID.class),
                        instant(rs, "kickoff_at"), rs.getObject("user_id", UUID.class), rs.getInt("points")))
                .list();
        return rows.stream()
                .collect(Collectors.groupingBy(ManagerPointsRow::matchId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(match -> new MatchdayPoints(match.getFirst().matchId(), match.getFirst().kickoffAt(),
                        match.stream()
                                .filter(row -> row.userId() != null)
                                .collect(Collectors.toMap(ManagerPointsRow::userId, ManagerPointsRow::points))))
                .toList();
    }

    @Override
    public void save(MatchScore score) {
        jdbc.sql("""
                        update scoring_match
                        set matchday = :matchday, kickoff_at = :kickoffAt, sheet_at = :sheetAt,
                            lineups_at = :lineupsAt, unlocked_at = :unlockedAt, rules = cast(:rules as jsonb),
                            scored_at = :scoredAt
                        where match_id = :matchId""")
                .param("matchId", score.matchId())
                .param("matchday", score.matchday())
                .param("kickoffAt", utc(score.kickoffAt()))
                .param("sheetAt", utc(score.sheetAt()))
                .param("lineupsAt", utc(score.lineupsAt()))
                .param("unlockedAt", utc(score.unlockedAt()))
                .param("rules", toJson(score.rules()))
                .param("scoredAt", utc(score.scoredAt()))
                .update();
        savePlayers(score);
        saveLineups(score);
    }

    private void savePlayers(MatchScore score) {
        jdbc.sql("delete from scoring_player_score where match_id = :matchId")
                .param("matchId", score.matchId())
                .update();
        Map<UUID, PlayerScore> points = byId(score.playerScores(), PlayerScore::playerId);
        batch.batchUpdate("""
                        insert into scoring_player_score (match_id, player_id, league_id, position, goals,
                            penalty_goals, penalties_saved, penalties_missed, assists, own_goals, yellow_card,
                            double_yellow, red_card, action_points, rating_points, points)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                score.sheet().stream()
                        .map(line -> {
                            PlayerScore scored = points.get(line.playerId());
                            return new Object[]{score.matchId(), line.playerId(), score.leagueId(),
                                    line.position().name(), line.goals(), line.penaltyGoals(), line.penaltiesSaved(),
                                    line.penaltiesMissed(), line.assists(), line.ownGoals(), line.yellowCard(),
                                    line.doubleYellow(), line.redCard(),
                                    nullableInt(scored == null ? null : scored.actionPoints()),
                                    scored == null ? 0 : scored.ratingPoints(),
                                    nullableInt(scored == null ? null : scored.points())};
                        })
                        .toList());
    }

    /** Deleting a lineup cascades to its players. */
    private void saveLineups(MatchScore score) {
        jdbc.sql("delete from scoring_lineup where match_id = :matchId")
                .param("matchId", score.matchId())
                .update();
        Map<UUID, ManagerScore> points = byId(score.managerScores(), ManagerScore::userId);
        batch.batchUpdate("""
                        insert into scoring_lineup (match_id, user_id, league_id, over_budget, points)
                        values (?, ?, ?, ?, ?)""",
                score.lineups().stream()
                        .map(lineup -> {
                            ManagerScore scored = points.get(lineup.userId());
                            return new Object[]{score.matchId(), lineup.userId(), score.leagueId(),
                                    lineup.overBudget(), nullableInt(scored == null ? null : scored.points())};
                        })
                        .toList());
        batch.batchUpdate("insert into scoring_lineup_player (match_id, user_id, player_id) values (?, ?, ?)",
                score.lineups().stream()
                        .flatMap(lineup -> lineup.playerIds().stream()
                                .map(playerId -> new Object[]{score.matchId(), lineup.userId(), playerId}))
                        .toList());
    }

    @Override
    public void delete(UUID matchId) {
        jdbc.sql("delete from scoring_match where match_id = :matchId")
                .param("matchId", matchId)
                .update();
    }

    private static <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static SqlParameterValue nullableInt(Integer value) {
        return new SqlParameterValue(Types.INTEGER, value);
    }

    private String toJson(ScoringTable rules) {
        if (rules == null) {
            return null;
        }
        try {
            return json.writeValueAsString(rules.points());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize scoring rules", e);
        }
    }

    private ScoringTable toRules(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new ScoringTable(json.readValue(value, RULES));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read scoring rules", e);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private MatchRow toMatchRow(ResultSet rs, int rowNum) throws SQLException {
        return new MatchRow(rs.getObject("match_id", UUID.class), rs.getObject("league_id", UUID.class),
                rs.getInt("matchday"), instant(rs, "kickoff_at"), instant(rs, "sheet_at"), instant(rs, "lineups_at"),
                instant(rs, "unlocked_at"), toRules(rs.getString("rules")), instant(rs, "scored_at"));
    }

    private static PlayerRow toPlayerRow(ResultSet rs, int rowNum) throws SQLException {
        return new PlayerRow(new PlayerPerformance(rs.getObject("player_id", UUID.class),
                Position.valueOf(rs.getString("position")), rs.getInt("goals"), rs.getInt("penalty_goals"),
                rs.getInt("penalties_saved"), rs.getInt("penalties_missed"), rs.getInt("assists"),
                rs.getInt("own_goals"), rs.getBoolean("yellow_card"), rs.getBoolean("double_yellow"),
                rs.getBoolean("red_card")), rs.getInt("action_points"), rs.getInt("rating_points"));
    }

    /** One row per lineup and player; a lineup without players has a single row with a null player. */
    private static LineupRow toLineupRow(ResultSet rs, int rowNum) throws SQLException {
        return new LineupRow(rs.getObject("user_id", UUID.class), rs.getBoolean("over_budget"), rs.getInt("points"),
                rs.getObject("player_id", UUID.class));
    }

    private record MatchRow(UUID matchId, UUID leagueId, int matchday, Instant kickoffAt, Instant sheetAt,
                            Instant lineupsAt, Instant unlockedAt, ScoringTable rules, Instant scoredAt) {
    }

    /** {@code actionPoints} is only read if the match is scored, when it is not null. */
    private record PlayerRow(PlayerPerformance performance, int actionPoints, int ratingPoints) {

        PlayerScore score() {
            return new PlayerScore(performance.playerId(), performance.position(), actionPoints, ratingPoints);
        }
    }

    /** {@code points} is only read if the match is scored, when it is not null. */
    private record LineupRow(UUID userId, boolean overBudget, int points, UUID playerId) {
    }

    private record ManagerPointsRow(UUID matchId, Instant kickoffAt, UUID userId, int points) {
    }
}
