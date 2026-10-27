package com.buddyfantasy.match.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.MatchSchedule;

/** Read-side port behind {@link com.buddyfantasy.match.MatchApi}: matches without their sheets, one query each. */
public interface MatchScheduleReadModel {

    /** Ordered by kickoff. */
    List<MatchSchedule> findByLeagueId(UUID leagueId);

    /** Scheduled matches of every league with {@code kickoffAt < until}, ordered by kickoff. */
    List<MatchSchedule> findScheduledKickingOffBefore(Instant until);
}
