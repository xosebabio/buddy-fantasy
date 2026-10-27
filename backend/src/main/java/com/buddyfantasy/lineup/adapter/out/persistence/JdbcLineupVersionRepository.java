package com.buddyfantasy.lineup.adapter.out.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.lineup.application.port.out.LineupVersionRepository;
import com.buddyfantasy.lineup.domain.LineupVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Versions are immutable, so plain inserts and one joined select are enough; no JPA entity. */
@Repository
class JdbcLineupVersionRepository implements LineupVersionRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    JdbcLineupVersionRepository(JdbcClient jdbc, JdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public List<LineupVersion> findByLeagueIdAndUserId(UUID leagueId, UUID userId) {
        List<VersionPlayerRow> rows = jdbc.sql("""
                        select v.id, v.saved_at, p.player_id
                        from lineup_version v
                        left join lineup_version_player p on p.version_id = v.id
                        where v.league_id = :leagueId and v.user_id = :userId
                        order by v.saved_at, v.id""")
                .param("leagueId", leagueId)
                .param("userId", userId)
                .query((rs, i) -> new VersionPlayerRow(rs.getObject("id", UUID.class),
                        rs.getObject("saved_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("player_id", UUID.class)))
                .list();
        Map<UUID, Instant> savedAt = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> playerIds = new LinkedHashMap<>();
        for (VersionPlayerRow row : rows) {
            savedAt.put(row.versionId(), row.savedAt());
            Set<UUID> players = playerIds.computeIfAbsent(row.versionId(), id -> new HashSet<>());
            if (row.playerId() != null) {
                players.add(row.playerId());
            }
        }
        return savedAt.entrySet().stream()
                .map(entry -> new LineupVersion(entry.getKey(), leagueId, userId, entry.getValue(),
                        playerIds.get(entry.getKey())))
                .toList();
    }

    @Override
    public void save(LineupVersion version) {
        jdbc.sql("""
                        insert into lineup_version (id, league_id, user_id, saved_at)
                        values (:id, :leagueId, :userId, :savedAt)""")
                .param("id", version.id())
                .param("leagueId", version.leagueId())
                .param("userId", version.userId())
                .param("savedAt", OffsetDateTime.ofInstant(version.savedAt(), ZoneOffset.UTC))
                .update();
        batch.batchUpdate("insert into lineup_version_player (version_id, player_id) values (?, ?)",
                version.playerIds().stream().map(playerId -> new Object[]{version.id(), playerId}).toList());
    }

    private record VersionPlayerRow(UUID versionId, Instant savedAt, UUID playerId) {
    }
}
