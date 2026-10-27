package com.buddyfantasy.lineup.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.match.MatchSchedule;

/**
 * A manager's working lineup over the league calendar (RF-25, RF-27, RF-28). The lineup of a match is the last
 * version saved strictly before its lock ({@code kickoffAt − lockHours}); what is saved later counts for the next
 * match. So an untouched lineup carries over by itself, and a postponement only moves the lock. A match already
 * locked keeps the lock it was locked at: changing {@code lockHours} only moves the locks still ahead, so the lineup
 * that counted for a locked match is always the one locked (and the baseline of changes). Matches are ordered
 * by kickoff, never by matchday number. Every instant comes from the caller (server clock). Only the versions saved
 * during the manager's current membership count ({@link LineupVersion#savedSince}): a manager who leaves the league
 * and comes back has no lineup until saving one, and that one has no change limit.
 */
public final class LineupTimeline {

    private final List<MatchSchedule> matches;
    private final int lockHours;
    private final Map<UUID, Instant> lockedAt;
    private final List<LineupVersion> versions;

    /**
     * {@code lockedAt} maps each match already locked to the lock instant it was locked with. {@code versions} are
     * one manager's in one league, member since {@code memberSince}.
     */
    public LineupTimeline(Collection<MatchSchedule> matches, int lockHours, Map<UUID, Instant> lockedAt,
            Collection<LineupVersion> versions, Instant memberSince) {
        this.matches = matches.stream().sorted(Comparator.comparing(MatchSchedule::kickoffAt)).toList();
        this.lockHours = lockHours;
        this.lockedAt = Map.copyOf(lockedAt);
        this.versions = versions.stream()
                .filter(version -> version.savedSince(memberSince))
                .sorted(Comparator.comparing(LineupVersion::savedAt))
                .toList();
    }

    public static Instant lockAt(Instant kickoffAt, int lockHours) {
        return kickoffAt.minus(Duration.ofHours(lockHours));
    }

    /** The instant the match was locked with if it is locked; otherwise, its kickoff minus the current lock hours. */
    public Instant lockAt(MatchSchedule match) {
        Instant locked = lockedAt.get(match.id());
        return locked != null ? locked : lockAt(match.kickoffAt(), lockHours);
    }

    /** The match a save made {@code now} counts for: the first one not closed whose lock is still ahead. */
    public Optional<MatchSchedule> targetMatch(Instant now) {
        return matches.stream()
                .filter(match -> !match.closed() && lockAt(match).isAfter(now))
                .findFirst();
    }

    /** The last match already locked at {@code now} ({@code lockAt ≤ now}), closed or not. */
    public Optional<MatchSchedule> lastLockedMatch(Instant now) {
        return matches.stream()
                .filter(match -> !lockAt(match).isAfter(now))
                .reduce((first, second) -> second);
    }

    /**
     * The lineup changes are counted against (RF-26): the one locked in {@link #lastLockedMatch}. Empty if no match
     * is locked yet or the manager had no lineup then, and then there is no limit.
     */
    public Optional<LineupVersion> baseline(Instant now) {
        return lastLockedMatch(now).flatMap(this::lineupFor);
    }

    /** The last version saved strictly before the match's lock; empty if the manager had none by then. */
    public Optional<LineupVersion> lineupFor(MatchSchedule match) {
        Instant lockAt = lockAt(match);
        return versions.stream()
                .filter(version -> version.savedAt().isBefore(lockAt))
                .reduce((first, second) -> second);
    }

    /** The working lineup: the last version saved. */
    public Optional<LineupVersion> current() {
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.getLast());
    }
}
