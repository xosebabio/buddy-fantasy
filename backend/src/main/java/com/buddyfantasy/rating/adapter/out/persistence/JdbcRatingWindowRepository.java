package com.buddyfantasy.rating.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.PlayerRatingResult;
import com.buddyfantasy.rating.domain.RatingRules;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.rating.domain.SheetPlayer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * A window is a small aggregate (~15 players), so saving it rewrites its players; no JPA entity. The players are
 * upserted instead of deleted and inserted again: deleting a player's row cascades to his votes, which only a player
 * removed from the sheet loses.
 */
@Repository
class JdbcRatingWindowRepository implements RatingWindowRepository {

    private static final String SELECT_WINDOW = """
            select match_id, league_id, matchday, kickoff_at, opened_at, closes_at, closed_at, format,
                   conversion_factor, conversion_base, min_votes, default_rating, sheet_at
            from rating_window
            where match_id = :matchId""";

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    JdbcRatingWindowRepository(JdbcClient jdbc, JdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public boolean saveIfAbsent(RatingWindow window) {
        // a concurrent insert of the same match waits for the other transaction and then does nothing
        int inserted = jdbc.sql("""
                        insert into rating_window (match_id, league_id, matchday, kickoff_at, opened_at, closes_at,
                            closed_at, format, conversion_factor, conversion_base, min_votes, default_rating, sheet_at)
                        values (:matchId, :leagueId, :matchday, :kickoffAt, :openedAt, :closesAt, :closedAt, :format,
                            :factor, :base, :minVotes, :defaultRating, :sheetAt)
                        on conflict (match_id) do nothing""")
                .param("matchId", window.matchId())
                .param("leagueId", window.leagueId())
                .param("matchday", window.matchday())
                .param("kickoffAt", utc(window.kickoffAt()))
                .param("openedAt", utc(window.openedAt()))
                .param("closesAt", utc(window.closesAt()))
                .param("closedAt", utc(window.closedAt()))
                .param("format", window.rules().format().name())
                .param("factor", window.rules().factor())
                .param("base", window.rules().base())
                .param("minVotes", window.rules().minVotes())
                .param("defaultRating", new SqlParameterValue(Types.NUMERIC, window.rules().defaultRating()))
                .param("sheetAt", utc(window.sheetAt()))
                .update();
        if (inserted == 0) {
            return false;
        }
        savePlayers(window);
        return true;
    }

    @Override
    public boolean exists(UUID matchId) {
        return jdbc.sql("select exists (select 1 from rating_window where match_id = :matchId)")
                .param("matchId", matchId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public Optional<RatingWindow> lock(UUID matchId) {
        return jdbc.sql(SELECT_WINDOW + " for update")
                .param("matchId", matchId)
                .query(JdbcRatingWindowRepository::toWindowRow)
                .optional()
                .map(this::load);
    }

    /** The players are read after the lock, so they include whatever the transaction that held it committed. */
    private RatingWindow load(WindowRow window) {
        List<PlayerRow> players = jdbc.sql("""
                        select player_id, position, votes, average, rating_points
                        from rating_player
                        where match_id = :matchId
                        order by player_id""")
                .param("matchId", window.matchId())
                .query(JdbcRatingWindowRepository::toPlayerRow)
                .list();
        Map<UUID, PlayerRatingResult> results = window.closedAt() == null ? Map.of() : players.stream()
                .collect(Collectors.toUnmodifiableMap(PlayerRow::playerId, PlayerRow::result));
        return new RatingWindow(window.matchId(), window.leagueId(), window.matchday(), window.kickoffAt(),
                window.openedAt(), window.closesAt(), window.closedAt(), window.rules(),
                players.stream().map(PlayerRow::player).toList(), window.sheetAt(), results);
    }

    @Override
    public void save(RatingWindow window) {
        jdbc.sql("update rating_window set closed_at = :closedAt, sheet_at = :sheetAt where match_id = :matchId")
                .param("matchId", window.matchId())
                .param("closedAt", utc(window.closedAt()))
                .param("sheetAt", utc(window.sheetAt()))
                .update();
        List<UUID> playerIds = window.players().stream().map(SheetPlayer::playerId).toList();
        if (playerIds.isEmpty()) {
            jdbc.sql("delete from rating_player where match_id = :matchId")
                    .param("matchId", window.matchId())
                    .update();
        } else {
            jdbc.sql("delete from rating_player where match_id = :matchId and player_id not in (:playerIds)")
                    .param("matchId", window.matchId())
                    .param("playerIds", playerIds)
                    .update();
        }
        savePlayers(window);
    }

    private void savePlayers(RatingWindow window) {
        batch.batchUpdate("""
                        insert into rating_player (match_id, player_id, position, votes, average, rating_points)
                        values (?, ?, ?, ?, ?, ?)
                        on conflict (match_id, player_id) do update
                        set position = excluded.position, votes = excluded.votes, average = excluded.average,
                            rating_points = excluded.rating_points""",
                window.players().stream()
                        .map(player -> {
                            PlayerRatingResult result = window.results().get(player.playerId());
                            return new Object[]{window.matchId(), player.playerId(), player.position().name(),
                                    new SqlParameterValue(Types.INTEGER, result == null ? null : result.votes()),
                                    new SqlParameterValue(Types.NUMERIC, result == null ? null : result.average()),
                                    new SqlParameterValue(Types.INTEGER,
                                            result == null ? null : result.ratingPoints())};
                        })
                        .toList());
    }

    @Override
    public List<UUID> findDueMatchIds(Instant now) {
        return jdbc.sql("""
                        select match_id from rating_window
                        where closed_at is null and closes_at <= :now
                        order by closes_at, match_id""")
                .param("now", utc(now))
                .query(UUID.class)
                .list();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static WindowRow toWindowRow(ResultSet rs, int rowNum) throws SQLException {
        return new WindowRow(rs.getObject("match_id", UUID.class), rs.getObject("league_id", UUID.class),
                rs.getInt("matchday"), instant(rs, "kickoff_at"), instant(rs, "opened_at"),
                instant(rs, "closes_at"), instant(rs, "closed_at"),
                new RatingRules(RatingFormat.valueOf(rs.getString("format")), rs.getBigDecimal("conversion_factor"),
                        rs.getBigDecimal("conversion_base"), rs.getInt("min_votes"),
                        rs.getBigDecimal("default_rating")),
                instant(rs, "sheet_at"));
    }

    private static PlayerRow toPlayerRow(ResultSet rs, int rowNum) throws SQLException {
        return new PlayerRow(rs.getObject("player_id", UUID.class), Position.valueOf(rs.getString("position")),
                rs.getObject("votes", Integer.class), rs.getBigDecimal("average"),
                rs.getObject("rating_points", Integer.class));
    }

    private record WindowRow(UUID matchId, UUID leagueId, int matchday, Instant kickoffAt, Instant openedAt,
                             Instant closesAt, Instant closedAt, RatingRules rules, Instant sheetAt) {
    }

    /** {@code votes} and {@code ratingPoints} are null until the window closes. */
    private record PlayerRow(UUID playerId, Position position, Integer votes, BigDecimal average,
                             Integer ratingPoints) {

        SheetPlayer player() {
            return new SheetPlayer(playerId, position);
        }

        PlayerRatingResult result() {
            return new PlayerRatingResult(votes, average, ratingPoints);
        }
    }
}
