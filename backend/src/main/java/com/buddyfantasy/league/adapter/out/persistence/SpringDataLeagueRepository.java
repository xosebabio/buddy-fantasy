package com.buddyfantasy.league.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLeagueRepository extends JpaRepository<LeagueEntity, UUID> {
}
