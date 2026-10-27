package com.buddyfantasy.match.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.match.application.port.out.MatchScheduleReadModel;
import org.springframework.stereotype.Service;

@Service
class MatchApiService implements MatchApi {

    private final MatchScheduleReadModel schedules;

    MatchApiService(MatchScheduleReadModel schedules) {
        this.schedules = schedules;
    }

    @Override
    public List<MatchSchedule> findMatches(UUID leagueId) {
        return schedules.findByLeagueId(leagueId);
    }

    @Override
    public List<MatchSchedule> findScheduledKickingOffBefore(Instant until) {
        return schedules.findScheduledKickingOffBefore(until);
    }
}
