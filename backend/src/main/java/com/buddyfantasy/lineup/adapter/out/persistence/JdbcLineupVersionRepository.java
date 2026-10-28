package com.buddyfantasy.lineup.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.lineup.application.port.out.LineupVersionRepository;
import com.buddyfantasy.lineup.domain.LineupVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Versions are immutable, so plain inserts and one joined select are enough; no JPA entity. */
@Repository
@RequiredArgsConstructor
class JdbcLineupVersionRepository implements LineupVersionRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    @Override
    public List<LineupVersion> findByLeagueIdAndUserId(UUID leagueId, UUID userId) {
        return toVersions(leagueId, jdbc.sql("""
                        select v.id, v.user_id, v.saved_at, p.player_id
                        from lineup_version v
                        left join lineup_version_player p on p.version_id = v.id
                        where v.league_id = :leagueId and v.user_id = :userId
                        order by v.saved_at, v.id""")
                .param("leagueId", leagueId)
                .param("userId", userId)
                .query(JdbcLineupVersionRepository::toRow)
                .list());
    }

    @Override
    public List<LineupVersion> findLastSavedBefore(UUID leagueId, Collection<UUID> userIds, Instant before) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        // same tie-break as findByLeagueIdAndUserId + LineupTimeline: the greatest id among equal instants
        return toVersions(leagueId, jdbc.sql("""
                        select v.id, v.user_id, v.saved_at, p.player_id
                        from (select distinct on (user_id) id, user_id, saved_at
                              from lineup_version
                              where league_id = :leagueId and user_id in (:userIds) and saved_at < :before
                              order by user_id, saved_at desc, id desc) v
                        left join lineup_version_player p on p.version_id = v.id
                        order by v.user_id""")
                .param("leagueId", leagueId)
                .param("userIds", userIds)
                .param("before", OffsetDateTime.ofInstant(before, ZoneOffset.UTC))
                .query(JdbcLineupVersionRepository::toRow)
                .list());
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

    /** One row per version and player, in version order; a version without players has a null player. */
    private static List<LineupVersion> toVersions(UUID leagueId, List<VersionPlayerRow> rows) {
        Map<UUID, VersionPlayerRow> versions = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> playerIds = new LinkedHashMap<>();
        for (VersionPlayerRow row : rows) {
            versions.putIfAbsent(row.versionId(), row);
            Set<UUID> players = playerIds.computeIfAbsent(row.versionId(), id -> new HashSet<>());
            if (row.playerId() != null) {
                players.add(row.playerId());
            }
        }
        return versions.values().stream()
                .map(row -> new LineupVersion(row.versionId(), leagueId, row.userId(), row.savedAt(),
                        playerIds.get(row.versionId())))
                .toList();
    }

    private static VersionPlayerRow toRow(ResultSet rs, int rowNum) throws SQLException {
        return new VersionPlayerRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("saved_at", OffsetDateTime.class).toInstant(), rs.getObject("player_id", UUID.class));
    }

    private record VersionPlayerRow(UUID versionId, UUID userId, Instant savedAt, UUID playerId) {
    }
}
