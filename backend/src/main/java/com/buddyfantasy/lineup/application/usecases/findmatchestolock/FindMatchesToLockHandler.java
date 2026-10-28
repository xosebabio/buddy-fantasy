package com.buddyfantasy.lineup.application.usecases.findmatchestolock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.lineup.domain.LineupTimeline;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Three queries whatever the number of leagues: no league locks earlier than {@code lineupLockHoursMax} before
 * kickoff, so later matches are not read. A league whose lock hours exceed a since-lowered maximum gets its matches
 * locked late; {@code MatchClosed} still locks them.
 */
@Component
@RequiredArgsConstructor
class FindMatchesToLockHandler implements QueryHandler<FindMatchesToLockQuery, List<MatchToLock>> {

    private final MatchApi matches;
    private final LeagueSettingsProvider leagueSettings;
    private final MatchLineupLockRepository locks;
    private final Clock clock;

    @Override
    public List<MatchToLock> handle(FindMatchesToLockQuery query) {
        Instant now = clock.instant();
        Instant lockedBy = now.minus(query.margin());
        List<MatchSchedule> scheduled = matches.findScheduledKickingOffBefore(
                now.plus(Duration.ofHours(leagueSettings.lineupLockHoursMax())));
        Map<UUID, LineupSettings> settings = leagueSettings.lineupSettings(
                scheduled.stream().map(MatchSchedule::leagueId).distinct().toList());
        List<MatchSchedule> due = scheduled.stream()
                .filter(match -> settings.containsKey(match.leagueId()))
                .filter(match -> !LineupTimeline.lockAt(match.kickoffAt(),
                        settings.get(match.leagueId()).lockHours()).isAfter(lockedBy))
                .toList();
        Set<UUID> locked = locks.findLockedMatchIds(due.stream().map(MatchSchedule::id).toList());
        return due.stream()
                .filter(match -> !locked.contains(match.id()))
                .map(match -> new MatchToLock(match.leagueId(), match.id()))
                .toList();
    }
}
