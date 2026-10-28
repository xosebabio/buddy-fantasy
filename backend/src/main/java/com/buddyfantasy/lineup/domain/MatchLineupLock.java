package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.LineupsLocked;
import com.buddyfantasy.lineup.LineupsUnlocked;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.shared.domain.AggregateRoot;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * The lock of a match's lineups (RF-25, RF-27), materialized once: it freezes the lineup that counted for each
 * member, the players' values at that instant and the budget check (RF-29), and publishes {@link LineupsLocked}.
 * A lineup over budget is locked anyway and scores 0 points. Uses the league's parameters current at the lock.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class MatchLineupLock extends AggregateRoot {

    private final UUID matchId;
    private final UUID leagueId;
    private final int matchday;
    private final Instant kickoffAt;
    private final Instant lockAt;
    private final Instant lockedAt;
    private final List<LockedLineup> lineups;

    /**
     * {@code lineups} are, for each current member who has one, the last version saved before the lock during the
     * current membership ({@link LineupTimeline#lineupFor}); {@code players} must contain all their players, retired
     * ones included. Throws {@link ConflictException} if the lock time has not come yet.
     */
    public static MatchLineupLock lock(MatchSchedule match, LineupSettings settings,
            Collection<LineupVersion> lineups, Map<UUID, LineupPlayer> players, Instant now) {
        Instant lockAt = LineupTimeline.lockAt(match.kickoffAt(), settings.lockHours());
        if (now.isBefore(lockAt)) {
            throw new ConflictException(LineupError.LOCK_NOT_DUE,
                    "The lineups of a match cannot be locked before " + lockAt);
        }
        LineupRules rules = new LineupRules(settings);
        Set<UUID> users = new HashSet<>();
        List<LockedLineup> locked = lineups.stream()
                .map(version -> {
                    requireBelongs(version, match, lockAt);
                    if (!users.add(version.userId())) {
                        throw new DomainValidationException("More than one lineup for user " + version.userId());
                    }
                    return lockedLineup(version, settings, rules, players);
                })
                .toList();
        MatchLineupLock lock = new MatchLineupLock(match.id(), match.leagueId(), match.matchday(), match.kickoffAt(),
                lockAt, now, locked);
        lock.register(new LineupsLocked(match.leagueId(), match.id(), match.matchday(), match.kickoffAt(),
                locked.stream().map(LockedLineup::toData).toList(), now));
        return lock;
    }

    /**
     * The match was postponed to {@code newKickoffAt}. If its lock moves to the future the lineups are no longer
     * locked: publishes {@link LineupsUnlocked} and returns true, and the caller deletes this lock so it is redone
     * with the versions saved before the new lock. Otherwise nothing changes.
     */
    public boolean reschedule(Instant newKickoffAt, int lockHours, Instant now) {
        if (!LineupTimeline.lockAt(newKickoffAt, lockHours).isAfter(now)) {
            return false;
        }
        register(new LineupsUnlocked(leagueId, matchId, now));
        return true;
    }

    private static void requireBelongs(LineupVersion version, MatchSchedule match, Instant lockAt) {
        if (!version.leagueId().equals(match.leagueId())) {
            throw new DomainValidationException("Lineup " + version.id() + " belongs to another league");
        }
        if (!version.savedAt().isBefore(lockAt)) {
            throw new DomainValidationException("Lineup " + version.id() + " was saved after the lock");
        }
    }

    private static LockedLineup lockedLineup(LineupVersion version, LineupSettings settings, LineupRules rules,
            Map<UUID, LineupPlayer> players) {
        List<LockedPlayer> lockedPlayers = version.playerIds().stream()
                .map(playerId -> {
                    LineupPlayer player = players.get(playerId);
                    if (player == null) {
                        throw new DomainValidationException("Player " + playerId + " is not in the league's squad");
                    }
                    return new LockedPlayer(playerId, player.currentValue());
                })
                .toList();
        BigDecimal totalValue = rules.totalValue(version.playerIds(), players);
        BigDecimal budget = settings.budgetEnabled() ? settings.budget() : null;
        return new LockedLineup(version.userId(), version.id(), lockedPlayers, totalValue, budget,
                rules.isOverBudget(totalValue));
    }
}
