package com.buddyfantasy.lineup.application.usecases.lockmatchlineups;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.application.port.out.LineupVersionRepository;
import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.lineup.application.port.out.PlayerCatalog;
import com.buddyfantasy.lineup.domain.LineupPlayer;
import com.buddyfantasy.lineup.domain.LineupTimeline;
import com.buddyfantasy.lineup.domain.LineupVersion;
import com.buddyfantasy.lineup.domain.MatchLineupLock;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Uses the current calendar, parameters and members: only current members' lineups count, each one the last
 * version saved before the lock, with the players' values current now for the budget check (RF-29). Only the
 * transaction that inserts the lock publishes the event.
 */
@Component
class LockMatchLineupsHandler implements CommandHandler<LockMatchLineupsCommand, Boolean> {

    private final LeagueAccess access;
    private final LeagueSettingsProvider leagueSettings;
    private final MatchApi matches;
    private final LineupVersionRepository versions;
    private final PlayerCatalog players;
    private final MatchLineupLockRepository locks;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LockMatchLineupsHandler(LeagueAccess access, LeagueSettingsProvider leagueSettings, MatchApi matches,
                            LineupVersionRepository versions, PlayerCatalog players, MatchLineupLockRepository locks,
                            ApplicationEventPublisher events, Clock clock) {
        this.access = access;
        this.leagueSettings = leagueSettings;
        this.matches = matches;
        this.versions = versions;
        this.players = players;
        this.locks = locks;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Boolean handle(LockMatchLineupsCommand command) {
        if (!locks.findLockedMatchIds(List.of(command.matchId())).isEmpty()) {
            return false;
        }
        Optional<MatchSchedule> match = matches.findMatches(command.leagueId()).stream()
                .filter(candidate -> candidate.id().equals(command.matchId()))
                .findFirst();
        if (match.isEmpty()) {
            return false;
        }
        LineupSettings settings = leagueSettings.lineupSettings(command.leagueId());
        Instant lockAt = LineupTimeline.lockAt(match.get().kickoffAt(), settings.lockHours());
        List<LineupVersion> lineups = versions.findLastSavedBefore(command.leagueId(),
                access.memberIds(command.leagueId()), lockAt);
        Set<UUID> playerIds = new HashSet<>();
        lineups.forEach(version -> playerIds.addAll(version.playerIds()));
        Map<UUID, LineupPlayer> lineupPlayers = players.findPlayers(command.leagueId(), playerIds);

        MatchLineupLock lock = MatchLineupLock.lock(match.get(), settings, lineups, lineupPlayers, clock.instant());
        if (!locks.saveIfAbsent(lock)) {
            return false;
        }
        lock.pullEvents().forEach(events::publishEvent);
        return true;
    }
}
