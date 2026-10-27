package com.buddyfantasy.match.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.match.domain.MatchStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMatchRepository extends JpaRepository<MatchEntity, UUID> {

    /** Match and sheet in a single query. */
    @EntityGraph(attributePaths = "performances")
    Optional<MatchEntity> findByLeagueIdAndId(UUID leagueId, UUID id);

    /** Without the sheets, which stay lazy. */
    List<MatchEntity> findByLeagueIdOrderByKickoffAt(UUID leagueId);

    /** Without the sheets, which stay lazy. */
    List<MatchEntity> findByStatusAndKickoffAtBeforeOrderByKickoffAt(MatchStatus status, Instant until);

    boolean existsByLeagueIdAndMatchday(UUID leagueId, short matchday);

    boolean existsByLeagueIdAndKickoffAt(UUID leagueId, Instant kickoffAt);
}
