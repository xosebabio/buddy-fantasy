package com.buddyfantasy.rating.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.buddyfantasy.league.RatingSettings;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.rating.PlayerRatingData;
import com.buddyfantasy.rating.RaterExcluded;
import com.buddyfantasy.rating.RatingWindowClosed;
import com.buddyfantasy.rating.RatingsCorrected;
import com.buddyfantasy.shared.domain.AggregateRoot;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The rating window of a closed match (RF-17): it opens with the match's {@code MatchClosed}, freezes the league's
 * {@link RatingRules} and closes {@code windowHours} later, when the job computes every player's result and publishes
 * {@link RatingWindowClosed}. The deadline is the server's clock: a vote at or after {@code closesAt} is rejected
 * although the job has not closed it yet. rating keeps its own copy of the sheet ({@code MatchClosed},
 * {@code MatchCorrected}): only a more recent event replaces it, and a player removed from it loses his votes.
 *
 * <p>{@code results} has one entry per player in the sheet once {@code closedAt} is set, and is empty before. The
 * votes are not part of the aggregate: they are only read to close it. Neither are the admin's exclusions (RF-41):
 * the caller reads them after locking the window, so it sees one committed while it waited.
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class RatingWindow extends AggregateRoot {

    private final UUID matchId;
    private final UUID leagueId;
    private final int matchday;
    private final Instant kickoffAt;
    private final Instant openedAt;
    private final Instant closesAt;
    private Instant closedAt;
    private final RatingRules rules;
    private List<SheetPlayer> players;
    private Instant sheetAt;
    private Map<UUID, PlayerRatingResult> results;

    /**
     * Opens the window of a {@code match} closed at {@code occurredAt} with its {@code sheet}, freezing the league's
     * current rating {@code settings}: it closes {@code windowHours} after.
     */
    public static RatingWindow open(MatchSchedule match, List<SheetPlayer> sheet, RatingSettings settings,
            Instant occurredAt) {
        if (!match.closed()) {
            throw new DomainValidationException("Match " + match.id() + " is not closed");
        }
        requireUnique(sheet);
        return new RatingWindow(match.id(), match.leagueId(), match.matchday(), match.kickoffAt(), occurredAt,
                occurredAt.plus(Duration.ofHours(settings.windowHours())), null, RatingRules.from(settings),
                List.copyOf(sheet), occurredAt, Map.of());
    }

    /**
     * Replaces the copy of the sheet if {@code occurredAt} is more recent than the one applied, so a
     * {@code MatchClosed} delivered again after a {@code MatchCorrected} does not undo the correction; the caller
     * deletes the votes of the players no longer in it. With the window not closed yet, the close will use the new
     * sheet. With the window closed it is not reopened (RF-16): the votes of a closed window never change, so a player
     * kept keeps his result, a player added counts as unrated and a player removed disappears; if that changes the
     * published results, publishes {@link RatingsCorrected} at {@code now}, after the {@link RatingWindowClosed}
     * whatever the order the events were delivered in. Returns whether it was applied.
     */
    public boolean applySheet(List<SheetPlayer> sheet, Instant occurredAt, Instant now) {
        if (!occurredAt.isAfter(sheetAt)) {
            return false;
        }
        requireUnique(sheet);
        List<PlayerRatingData> published = data();
        players = List.copyOf(sheet);
        sheetAt = occurredAt;
        if (closedAt == null) {
            return true;
        }
        results = players.stream().collect(Collectors.toUnmodifiableMap(SheetPlayer::playerId,
                player -> results.getOrDefault(player.playerId(), RatingConversion.result(rules, List.of()))));
        List<PlayerRatingData> corrected = data();
        if (!new HashSet<>(corrected).equals(new HashSet<>(published))) {
            register(new RatingsCorrected(leagueId, matchId, corrected, now));
        }
        return true;
    }

    /** Whether votes are accepted at {@code now}: not closed and before {@code closesAt}. */
    public boolean isOpen(Instant now) {
        return status(now) == RatingStatus.OPEN;
    }

    public RatingStatus status(Instant now) {
        return RatingStatus.of(closesAt, closedAt, now);
    }

    /** Throws {@link ConflictException} with {@link RatingError#RATING_WINDOW_CLOSED} unless {@link #isOpen}. */
    public void requireOpen(Instant now) {
        if (!isOpen(now)) {
            throw new ConflictException(RatingError.RATING_WINDOW_CLOSED,
                    "The ratings of match " + matchId + " closed at " + closesAt);
        }
    }

    public boolean closed() {
        return closedAt != null;
    }

    /**
     * RF-41: the admin ({@code adminId}) cancels the ratings of a member ({@code userId}) in the match, also of one
     * who has not rated it yet: the caller deletes their votes and saves the exclusion returned, and the member can
     * no longer rate it. Only with the window open at {@code now} ({@link RatingError#RATING_WINDOW_CLOSED}, 409).
     * Definitive and idempotent: a member {@code alreadyExcluded} returns empty and publishes nothing; otherwise
     * publishes {@link RaterExcluded}.
     */
    public Optional<RaterExclusion> exclude(UUID userId, UUID adminId, boolean alreadyExcluded, Instant now) {
        requireOpen(now);
        if (alreadyExcluded) {
            return Optional.empty();
        }
        register(new RaterExcluded(leagueId, matchId, userId, now));
        return Optional.of(new RaterExclusion(matchId, userId, adminId, now));
    }

    public boolean hasPlayer(UUID playerId) {
        return players.stream().anyMatch(player -> player.playerId().equals(playerId));
    }

    /**
     * Closes the window if its time is up: computes the result of every player in the sheet with the frozen rules
     * and the votes that remain ({@code votesByPlayer}: values by player; players without votes may be missing) and
     * publishes {@link RatingWindowClosed}. Does nothing if already closed or before {@code closesAt}. Returns whether
     * it closed it.
     */
    public boolean close(Map<UUID, List<Integer>> votesByPlayer, Instant now) {
        if (closedAt != null || now.isBefore(closesAt)) {
            return false;
        }
        results = players.stream().collect(Collectors.toUnmodifiableMap(SheetPlayer::playerId,
                player -> RatingConversion.result(rules, votesByPlayer.getOrDefault(player.playerId(), List.of()))));
        closedAt = now;
        register(new RatingWindowClosed(leagueId, matchId, data(), now));
        return true;
    }

    /** The results in the order of the sheet, as published; empty while not closed. */
    private List<PlayerRatingData> data() {
        if (results.isEmpty()) {
            return List.of();
        }
        return players.stream().map(player -> results.get(player.playerId()).toData(player.playerId())).toList();
    }

    private static void requireUnique(List<SheetPlayer> sheet) {
        if (sheet.stream().map(SheetPlayer::playerId).distinct().count() != sheet.size()) {
            throw new DomainValidationException("More than one player in the sheet with the same id");
        }
    }
}
