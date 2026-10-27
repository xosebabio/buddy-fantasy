package com.buddyfantasy.match.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.match.application.port.out.MatchScheduleReadModel;
import com.buddyfantasy.match.domain.MatchStatus;
import org.springframework.stereotype.Repository;

/** The sheet collection is lazy and never touched, so each method is a single query on {@code match_match}. */
@Repository
class JpaMatchScheduleReadModel implements MatchScheduleReadModel {

    private final SpringDataMatchRepository jpa;

    JpaMatchScheduleReadModel(SpringDataMatchRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<MatchSchedule> findByLeagueId(UUID leagueId) {
        return jpa.findByLeagueIdOrderByKickoffAt(leagueId).stream().map(MatchMapper::toSchedule).toList();
    }

    @Override
    public List<MatchSchedule> findScheduledKickingOffBefore(Instant until) {
        return jpa.findByStatusAndKickoffAtBeforeOrderByKickoffAt(MatchStatus.SCHEDULED, until).stream()
                .map(MatchMapper::toSchedule)
                .toList();
    }
}
