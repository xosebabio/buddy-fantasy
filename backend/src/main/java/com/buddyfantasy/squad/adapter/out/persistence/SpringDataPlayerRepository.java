package com.buddyfantasy.squad.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    Optional<PlayerEntity> findByLeagueIdAndId(UUID leagueId, UUID id);

    List<PlayerEntity> findByLeagueId(UUID leagueId);

    List<PlayerEntity> findByLeagueIdAndRetiredAtIsNull(UUID leagueId);

    List<PlayerEntity> findByLeagueIdAndIdIn(UUID leagueId, Collection<UUID> ids);

    boolean existsByLeagueIdAndShirtNumberAndRetiredAtIsNull(UUID leagueId, Short shirtNumber);
}
