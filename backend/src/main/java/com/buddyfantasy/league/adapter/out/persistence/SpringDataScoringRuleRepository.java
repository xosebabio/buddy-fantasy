package com.buddyfantasy.league.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataScoringRuleRepository extends JpaRepository<ScoringRuleEntity, ScoringRuleKey> {

    List<ScoringRuleEntity> findByLeagueId(UUID leagueId);
}
