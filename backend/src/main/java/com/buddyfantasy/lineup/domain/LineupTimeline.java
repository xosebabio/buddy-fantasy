package com.buddyfantasy.lineup.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.buddyfantasy.match.MatchSchedule;

/**
 * A manager's working lineup over the league calendar (RF-25, RF-27, RF-28). The lineup of a match is the last
 * version saved strictly before its lock ({@code kickoffAt − lockHours}); what is saved later counts for the next
 * match. So an untouched lineup carries over by itself, and a postponement only moves the lock. Matches are ordered
 * by kickoff, never by matchday number. Every instant comes from the caller (server clock).
 */
public final class LineupTimeline {

    private final List<MatchSchedule> matches;
    private final int lockHours;
    private final List<LineupVersion> versions;

    /** {@code versions} are one manager's in one league. */
    public LineupTimeline(Collection<MatchSchedule> matches, int lockHours, Collection<LineupVersion> versions) {
        this.matches = matches.stream().sorted(Comparator.comparing(MatchSchedule::kickoffAt)).toList();
        this.lockHours = lockHours;
        this.versions = versions.stream().sorted(Comparator.comparing(LineupVersion::savedAt)).toList();
    }

    public static Instant lockAt(Instant kickoffAt, int lockHours) {
        return kickoffAt.minus(Duration.ofHours(lockHours));
    }

    public Instant lockAt(MatchSchedule match) {
        return lockAt(match.kickoffAt(), lockHours);
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
