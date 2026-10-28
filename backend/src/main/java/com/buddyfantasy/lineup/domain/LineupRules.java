package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.RequiredArgsConstructor;

/**
 * The lineup rules of a league (RF-24, RF-26, RF-29) with its current parameters. {@code players} maps every player
 * id involved to its current data; ids missing from it are unknown or belong to another league.
 */
@RequiredArgsConstructor
public final class LineupRules {

    private final LineupSettings settings;

    /**
     * A new lineup: exactly {@code size} different players, all active and of the league, the minimum per position
     * if the formation is enabled, and within the budget (the limit included) if it is enabled. Throws
     * {@link DomainValidationException} with a {@link LineupError} code.
     */
    public void requireValid(List<UUID> playerIds, Map<UUID, LineupPlayer> players) {
        requireShape(playerIds);
        for (UUID playerId : playerIds) {
            LineupPlayer player = players.get(playerId);
            if (player == null) {
                throw invalid(LineupError.UNKNOWN_PLAYER, "Player " + playerId + " is not in the league's squad");
            }
            if (!player.active()) {
                throw invalid(LineupError.INACTIVE_PLAYER, "Player " + playerId + " was retired");
            }
        }
        if (settings.formationEnabled()) {
            requireFormation(playerIds, players);
        }
        if (isOverBudget(totalValue(playerIds, players))) {
            throw invalid(LineupError.OVER_BUDGET, "The lineup exceeds the budget of " + settings.budget());
        }
    }

    /**
     * Exactly {@code size} different players, checked without the players' data: a malformed lineup is rejected
     * before looking them up. Part of {@link #requireValid}.
     */
    public void requireShape(List<UUID> playerIds) {
        if (new HashSet<>(playerIds).size() != playerIds.size()) {
            throw invalid(LineupError.DUPLICATE_PLAYER, "A player appears more than once in the lineup");
        }
        if (playerIds.size() != settings.size()) {
            throw invalid(LineupError.SIZE, "A lineup has exactly " + settings.size() + " players");
        }
    }

    /**
     * Changes against the baseline (RF-26): new players that were not in it, counted on the final difference.
     * Replacing a retired player of the baseline is free, and so is filling the gap if {@code size} grew. Without a
     * baseline (first lineup) nothing counts.
     */
    public int changes(Optional<LineupVersion> baseline, Collection<UUID> playerIds,
            Map<UUID, LineupPlayer> players) {
        if (baseline.isEmpty()) {
            return 0;
        }
        Set<UUID> previous = baseline.get().playerIds();
        Set<UUID> current = Set.copyOf(playerIds);
        long added = current.stream().filter(id -> !previous.contains(id)).count();
        long retired = previous.stream().filter(id -> !current.contains(id) && !isActive(id, players)).count();
        long gap = Math.max(0, settings.size() - previous.size());
        return (int) Math.max(0, added - retired - gap);
    }

    /** Throws {@link ConflictException} with {@link LineupError#MAX_CHANGES_EXCEEDED} above {@code maxChanges}. */
    public void requireWithinMaxChanges(Optional<LineupVersion> baseline, Collection<UUID> playerIds,
            Map<UUID, LineupPlayer> players) {
        int changes = changes(baseline, playerIds, players);
        if (changes > settings.maxChanges()) {
            throw new ConflictException(LineupError.MAX_CHANGES_EXCEEDED, "The lineup has " + changes
                    + " changes and at most " + settings.maxChanges() + " are allowed per matchday");
        }
    }

    /** Sum of the current values of the known players, in millions. */
    public BigDecimal totalValue(Collection<UUID> playerIds, Map<UUID, LineupPlayer> players) {
        return playerIds.stream().distinct()
                .map(players::get)
                .filter(Objects::nonNull)
                .map(LineupPlayer::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Above the budget, if enabled; exactly the budget fits. */
    public boolean isOverBudget(BigDecimal totalValue) {
        return settings.budgetEnabled() && totalValue.compareTo(settings.budget()) > 0;
    }

    /** What a saved lineup no longer meets with the current values, squad and parameters. */
    public Set<LineupWarning> warnings(Collection<UUID> playerIds, Map<UUID, LineupPlayer> players) {
        Set<LineupWarning> warnings = EnumSet.noneOf(LineupWarning.class);
        if (isOverBudget(totalValue(playerIds, players))) {
            warnings.add(LineupWarning.OVER_BUDGET);
        }
        if (playerIds.stream().anyMatch(id -> !isActive(id, players))) {
            warnings.add(LineupWarning.RETIRED_PLAYERS);
        }
        if (playerIds.size() != settings.size()) {
            warnings.add(LineupWarning.SIZE_MISMATCH);
        }
        return warnings;
    }

    private void requireFormation(List<UUID> playerIds, Map<UUID, LineupPlayer> players) {
        Map<Position, Integer> counts = new EnumMap<>(Position.class);
        playerIds.forEach(id -> counts.merge(players.get(id).position(), 1, Integer::sum));
        for (Position position : Position.values()) {
            int minimum = minimum(position);
            if (counts.getOrDefault(position, 0) < minimum) {
                throw invalid(LineupError.FORMATION,
                        "The lineup needs at least " + minimum + " " + position + " player(s)");
            }
        }
    }

    private int minimum(Position position) {
        return switch (position) {
            case GOALKEEPER -> settings.minGoalkeepers();
            case DEFENDER -> settings.minDefenders();
            case MIDFIELDER -> settings.minMidfielders();
            case FORWARD -> settings.minForwards();
        };
    }

    /** Missing players count as retired: they can no longer play. */
    private static boolean isActive(UUID playerId, Map<UUID, LineupPlayer> players) {
        LineupPlayer player = players.get(playerId);
        return player != null && player.active();
    }

    private static DomainValidationException invalid(LineupError code, String message) {
        return new DomainValidationException(code, message);
    }
}
