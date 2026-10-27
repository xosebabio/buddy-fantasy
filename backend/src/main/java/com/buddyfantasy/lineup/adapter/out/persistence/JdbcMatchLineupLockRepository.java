package com.buddyfantasy.lineup.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.lineup.domain.LockedLineup;
import com.buddyfantasy.lineup.domain.LockedPlayer;
import com.buddyfantasy.lineup.domain.MatchLineupLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Locks are written once and deleted to unlock, so plain inserts and one joined select are enough; no JPA entity.
 * Deleting the lock row cascades to its lineups and players.
 */
@Repository
class JdbcMatchLineupLockRepository implements MatchLineupLockRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    JdbcMatchLineupLockRepository(JdbcClient jdbc, JdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public boolean saveIfAbsent(MatchLineupLock lock) {
        // a concurrent insert of the same match waits for the other transaction and then does nothing
        int inserted = jdbc.sql("""
                        insert into lineup_match_lock (match_id, league_id, matchday, kickoff_at, lock_at, locked_at)
                        values (:matchId, :leagueId, :matchday, :kickoffAt, :lockAt, :lockedAt)
                        on conflict (match_id) do nothing""")
                .param("matchId", lock.matchId())
                .param("leagueId", lock.leagueId())
                .param("matchday", lock.matchday())
                .param("kickoffAt", utc(lock.kickoffAt()))
                .param("lockAt", utc(lock.lockAt()))
                .param("lockedAt", utc(lock.lockedAt()))
                .update();
        if (inserted == 0) {
            return false;
        }
        batch.batchUpdate("""
                        insert into lineup_locked_lineup (match_id, user_id, version_id, total_value, budget, over_budget)
                        values (?, ?, ?, ?, ?, ?)""",
                lock.lineups().stream()
                        .map(lineup -> new Object[]{lock.matchId(), lineup.userId(), lineup.versionId(),
                                lineup.totalValue(), new SqlParameterValue(Types.NUMERIC, lineup.budget()),
                                lineup.overBudget()})
                        .toList());
        batch.batchUpdate("insert into lineup_locked_player (match_id, user_id, player_id, value) values (?, ?, ?, ?)",
                lock.lineups().stream()
                        .flatMap(lineup -> lineup.players().stream()
                                .map(player -> new Object[]{lock.matchId(), lineup.userId(), player.playerId(),
                                        player.value()}))
                        .toList());
        return true;
    }

    @Override
    public Optional<MatchLineupLock> findByMatchId(UUID matchId) {
        List<LockRow> rows = jdbc.sql("""
                        select m.match_id, m.league_id, m.matchday, m.kickoff_at, m.lock_at, m.locked_at,
                               l.user_id, l.version_id, l.total_value, l.budget, l.over_budget, p.player_id, p.value
                        from lineup_match_lock m
                        left join lineup_locked_lineup l on l.match_id = m.match_id
                        left join lineup_locked_player p on p.match_id = l.match_id and p.user_id = l.user_id
                        where m.match_id = :matchId
                        order by l.user_id, p.player_id""")
                .param("matchId", matchId)
                .query(JdbcMatchLineupLockRepository::toRow)
                .list();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<UUID, LockRow> lineups = new LinkedHashMap<>();
        Map<UUID, List<LockedPlayer>> players = new LinkedHashMap<>();
        for (LockRow row : rows) {
            if (row.userId() == null) {
                continue;
            }
            lineups.putIfAbsent(row.userId(), row);
            List<LockedPlayer> lineupPlayers = players.computeIfAbsent(row.userId(), id -> new ArrayList<>());
            if (row.playerId() != null) {
                lineupPlayers.add(new LockedPlayer(row.playerId(), row.value()));
            }
        }
        LockRow lock = rows.getFirst();
        return Optional.of(new MatchLineupLock(lock.matchId(), lock.leagueId(), lock.matchday(), lock.kickoffAt(),
                lock.lockAt(), lock.lockedAt(), lineups.values().stream()
                .map(row -> new LockedLineup(row.userId(), row.versionId(), players.get(row.userId()),
                        row.totalValue(), row.budget(), row.overBudget()))
                .toList()));
    }

    @Override
    public Set<UUID> findLockedMatchIds(Collection<UUID> matchIds) {
        if (matchIds.isEmpty()) {
            return Set.of();
        }
        return jdbc.sql("select match_id from lineup_match_lock where match_id in (:matchIds)")
                .param("matchIds", matchIds)
                .query(UUID.class)
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Map<UUID, Instant> findLockAtsByLeagueId(UUID leagueId) {
        return jdbc.sql("select match_id, lock_at from lineup_match_lock where league_id = :leagueId")
                .param("leagueId", leagueId)
                .query((rs, rowNum) -> Map.entry(rs.getObject("match_id", UUID.class),
                        rs.getObject("lock_at", OffsetDateTime.class).toInstant()))
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void deleteByMatchId(UUID matchId) {
        jdbc.sql("delete from lineup_match_lock where match_id = :matchId")
                .param("matchId", matchId)
                .update();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    /** One row per lineup and player; a lock without lineups has a single row with null lineup columns. */
    private static LockRow toRow(ResultSet rs, int rowNum) throws SQLException {
        return new LockRow(rs.getObject("match_id", UUID.class), rs.getObject("league_id", UUID.class),
                rs.getInt("matchday"), instant(rs, "kickoff_at"), instant(rs, "lock_at"), instant(rs, "locked_at"),
                rs.getObject("user_id", UUID.class), rs.getObject("version_id", UUID.class),
                rs.getBigDecimal("total_value"), rs.getBigDecimal("budget"), rs.getBoolean("over_budget"),
                rs.getObject("player_id", UUID.class), rs.getBigDecimal("value"));
    }

    private record LockRow(UUID matchId, UUID leagueId, int matchday, Instant kickoffAt, Instant lockAt,
                           Instant lockedAt, UUID userId, UUID versionId, BigDecimal totalValue, BigDecimal budget,
                           boolean overBudget, UUID playerId, BigDecimal value) {
    }
}
