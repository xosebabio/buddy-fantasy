package com.buddyfantasy.match.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMatchRepository extends JpaRepository<MatchEntity, UUID> {

    /** Match and sheet in a single query. */
    @EntityGraph(attributePaths = "performances")
    Optional<MatchEntity> findByLeagueIdAndId(UUID leagueId, UUID id);

    boolean existsByLeagueIdAndMatchday(UUID leagueId, short matchday);

    boolean existsByLeagueIdAndKickoffAt(UUID leagueId, Instant kickoffAt);
}
