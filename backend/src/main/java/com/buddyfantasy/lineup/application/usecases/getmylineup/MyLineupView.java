package com.buddyfantasy.lineup.application.usecases.getmylineup;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.domain.LineupPlayer;
import com.buddyfantasy.lineup.domain.LineupRules;
import com.buddyfantasy.lineup.domain.LineupTimeline;
import com.buddyfantasy.lineup.domain.LineupVersion;
import com.buddyfantasy.lineup.domain.LineupWarning;

/**
 * A manager's working lineup at the server's {@code now} (RF-24 to RF-29). Before the first save {@code savedAt}
 * is null and {@code players} is empty. {@code targetMatch} is null if no match is ahead. Changes are counted
 * against the lineup locked in the last locked match; {@code changesRemaining} is null while there is no limit
 * (no lineup locked yet). {@code budget} is null if disabled; amounts are in millions. {@code warnings} are codes
 * that never block anything.
 */
public record MyLineupView(Instant savedAt, List<LineupPlayerView> players, TargetMatchView targetMatch,
                           int maxChanges, int changesUsed, Integer changesRemaining, BigDecimal totalValue,
                           BigDecimal budget, List<LineupWarning> warnings) {

    /** Goalkeepers first, as the squad. */
    private static final Comparator<LineupPlayerView> LINEUP_ORDER = Comparator.comparing(LineupPlayerView::position)
            .thenComparing(LineupPlayerView::id);

    /** {@code players} must contain the players of the current version and of the baseline. */
    public static MyLineupView from(LineupTimeline timeline, LineupSettings settings,
                                    Map<UUID, LineupPlayer> players, Instant now) {
        LineupRules rules = new LineupRules(settings);
        Optional<LineupVersion> current = timeline.current();
        Optional<LineupVersion> baseline = timeline.baseline(now);
        Set<UUID> playerIds = current.map(LineupVersion::playerIds).orElse(Set.of());
        int changesUsed = rules.changes(baseline, playerIds, players);
        return new MyLineupView(
                current.map(LineupVersion::savedAt).orElse(null),
                playerIds.stream().map(players::get).filter(Objects::nonNull).map(LineupPlayerView::from)
                        .sorted(LINEUP_ORDER).toList(),
                timeline.targetMatch(now)
                        .map(match -> new TargetMatchView(match.id(), match.matchday(), match.kickoffAt(),
                                timeline.lockAt(match)))
                        .orElse(null),
                settings.maxChanges(),
                changesUsed,
                baseline.isPresent() ? Math.max(0, settings.maxChanges() - changesUsed) : null,
                rules.totalValue(playerIds, players),
                settings.budgetEnabled() ? settings.budget() : null,
                current.isPresent() ? List.copyOf(rules.warnings(playerIds, players)) : List.of());
    }
}
