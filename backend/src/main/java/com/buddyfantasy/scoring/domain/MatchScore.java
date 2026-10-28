package com.buddyfantasy.scoring.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.scoring.MatchdayScored;
import com.buddyfantasy.shared.domain.AggregateRoot;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The scoring of a match (RF-15, RF-30, RF-31): scoring's own copy of its sheet ({@code MatchClosed},
 * {@code MatchCorrected}), of its locked lineups ({@code LineupsLocked}) and of its points for ratings
 * ({@code RatingWindowClosed}, {@code RatingsCorrected}), and the points computed from the three. Events arrive at
 * least once and in any order, so each copy keeps the {@code occurredAt} of the event it came from and only a more
 * recent event replaces it; an older or repeated one changes nothing. The match is scored once it has the three
 * copies, so not before its rating window closes, with the league's table frozen the first time (a correction reuses
 * it), and publishes {@link MatchdayScored} the first time and whenever a recalculation changes what it published.
 *
 * <p>{@code sheet}, {@code lineups} and {@code ratingPoints} are empty while {@code sheetAt}, {@code lineupsAt} and
 * {@code ratingsAt} are null; {@code playerScores} and {@code managerScores} are empty while {@code scoredAt} is
 * null.
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class MatchScore extends AggregateRoot {

    private final UUID matchId;
    private final UUID leagueId;
    private int matchday;
    private Instant kickoffAt;
    private List<PlayerPerformance> sheet;
    private Instant sheetAt;
    private List<ManagerLineup> lineups;
    private Instant lineupsAt;
    private Instant unlockedAt;
    /** Points for ratings by player; a player of the sheet without them scores 0 for ratings. */
    private Map<UUID, Integer> ratingPoints;
    private Instant ratingsAt;
    private ScoringTable rules;
    private Instant scoredAt;
    private List<PlayerScore> playerScores;
    private List<ManagerScore> managerScores;

    /** A match scoring has heard of, with no copy yet. */
    public static MatchScore create(UUID matchId, UUID leagueId, int matchday, Instant kickoffAt) {
        return new MatchScore(matchId, leagueId, matchday, kickoffAt, List.of(), null, List.of(), null, null,
                Map.of(), null, null, null, List.of(), List.of());
    }

    /**
     * Replaces the copy of the sheet if {@code occurredAt} is more recent than the one applied, so a
     * {@code MatchClosed} delivered again after a {@code MatchCorrected} does not undo the correction. The sheet's
     * event also brings the matchday and kickoff, which no longer change once the match is closed: a lineup lock
     * may have been published before a postponement that kept it locked. Returns whether it was applied.
     */
    public boolean applySheet(int newMatchday, Instant newKickoffAt, List<PlayerPerformance> performances,
            Instant occurredAt) {
        if (!isNewer(occurredAt, sheetAt)) {
            return false;
        }
        requireUnique(performances, PlayerPerformance::playerId, "player in the sheet");
        matchday = newMatchday;
        kickoffAt = newKickoffAt;
        sheet = List.copyOf(performances);
        sheetAt = occurredAt;
        return true;
    }

    /**
     * Replaces the copy of the locked lineups if {@code occurredAt} is more recent than both the lineups applied and
     * the last unlock: a {@code LineupsLocked} delivered again after a {@code LineupsUnlocked} is ignored. Returns
     * whether it was applied.
     */
    public boolean applyLineups(List<ManagerLineup> locked, Instant occurredAt) {
        if (!isNewer(occurredAt, lineupsAt) || !isNewer(occurredAt, unlockedAt)) {
            return false;
        }
        requireUnique(locked, ManagerLineup::userId, "lineup of user");
        lineups = List.copyOf(locked);
        lineupsAt = occurredAt;
        return true;
    }

    /**
     * The match was postponed and its lineups unlocked: drops the copy of lineups locked before {@code occurredAt}
     * and remembers the unlock. A match is only unlocked before it is closed, so a scored match ignores it and never
     * loses its points. Returns whether it was applied.
     */
    public boolean unlock(Instant occurredAt) {
        if (scoredAt != null || !isNewer(occurredAt, unlockedAt)) {
            return false;
        }
        unlockedAt = occurredAt;
        if (lineupsAt != null && lineupsAt.isBefore(occurredAt)) {
            lineups = List.of();
            lineupsAt = null;
        }
        return true;
    }

    /**
     * Replaces the copy of the points for ratings if {@code occurredAt} is more recent than the one applied, so a
     * {@code RatingWindowClosed} delivered again after a {@code RatingsCorrected} does not undo the correction. It may
     * have players the sheet does not have yet, or the other way round. Returns whether it was applied.
     */
    public boolean applyRatings(Map<UUID, Integer> pointsByPlayer, Instant occurredAt) {
        if (!isNewer(occurredAt, ratingsAt)) {
            return false;
        }
        ratingPoints = Map.copyOf(pointsByPlayer);
        ratingsAt = occurredAt;
        return true;
    }

    public boolean hasSheet() {
        return sheetAt != null;
    }

    public boolean scored() {
        return scoredAt != null;
    }

    /**
     * Scores the match if it has the three copies; otherwise does nothing. The first time it freezes the league's
     * {@code currentTable} (only read then); later it reuses the frozen one. Every player in the sheet gets their
     * points for actions plus those for ratings, without weights (0 for ratings if the copy does not have them: a
     * player added by a correction until its {@code RatingsCorrected} arrives), and every manager the sum of their
     * lineup's players (0 for one who did not play) or 0 if over budget. Publishes {@link MatchdayScored} the first
     * time and, later, only if something changed: scoring the same data twice changes nothing.
     */
    public void score(Supplier<ScoringTable> currentTable, Instant now) {
        if (sheetAt == null || lineupsAt == null || ratingsAt == null) {
            return;
        }
        ScoringTable table = rules != null ? rules : currentTable.get();
        List<PlayerScore> players = sheet.stream()
                .map(performance -> new PlayerScore(performance.playerId(), performance.position(),
                        ActionScoring.breakdown(performance, table).total(),
                        ratingPoints.getOrDefault(performance.playerId(), 0)))
                .toList();
        Map<UUID, Integer> pointsByPlayer = players.stream()
                .collect(Collectors.toMap(PlayerScore::playerId, PlayerScore::points));
        List<ManagerScore> managers = lineups.stream()
                .map(lineup -> new ManagerScore(lineup.userId(), lineup.overBudget() ? 0 : lineup.playerIds().stream()
                        .mapToInt(playerId -> pointsByPlayer.getOrDefault(playerId, 0))
                        .sum(), lineup.overBudget()))
                .toList();
        if (scoredAt != null && sameElements(players, playerScores) && sameElements(managers, managerScores)) {
            return;
        }
        rules = table;
        playerScores = players;
        managerScores = managers;
        scoredAt = now;
        register(new MatchdayScored(leagueId, matchId, matchday, kickoffAt,
                players.stream().map(PlayerScore::toData).toList(),
                managers.stream().map(ManagerScore::toData).toList(), now));
    }

    /** The managers' points of the match for {@link Standings}; none while it is not scored. */
    public MatchdayPoints matchdayPoints() {
        return new MatchdayPoints(matchId, kickoffAt, managerScores.stream()
                .collect(Collectors.toMap(ManagerScore::userId, ManagerScore::points)));
    }

    private static boolean isNewer(Instant occurredAt, Instant applied) {
        return applied == null || occurredAt.isAfter(applied);
    }

    private static <T> boolean sameElements(List<T> computed, List<T> current) {
        return new HashSet<>(computed).equals(new HashSet<>(current));
    }

    private static <T> void requireUnique(List<T> items, Function<T, UUID> id, String what) {
        if (items.stream().map(id).distinct().count() != items.size()) {
            throw new DomainValidationException("More than one " + what + " with the same id");
        }
    }
}
