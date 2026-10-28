package com.buddyfantasy.lineup.application.usecases.reschedulematchlock;

import java.time.Clock;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.lineup.domain.MatchLineupLock;
import com.buddyfantasy.shared.application.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** The new lock time uses the league's current lock hours and the server clock. */
@Component
@RequiredArgsConstructor
class RescheduleMatchLockHandler implements CommandHandler<RescheduleMatchLockCommand, Void> {

    private final LeagueSettingsProvider leagueSettings;
    private final MatchLineupLockRepository locks;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(RescheduleMatchLockCommand command) {
        locks.findByMatchId(command.matchId()).ifPresent(lock -> reschedule(lock, command));
        return null;
    }

    private void reschedule(MatchLineupLock lock, RescheduleMatchLockCommand command) {
        int lockHours = leagueSettings.lineupSettings(lock.leagueId()).lockHours();
        if (lock.reschedule(command.kickoffAt(), lockHours, clock.instant())) {
            locks.deleteByMatchId(lock.matchId());
            lock.pullEvents().forEach(events::publishEvent);
        }
    }
}
