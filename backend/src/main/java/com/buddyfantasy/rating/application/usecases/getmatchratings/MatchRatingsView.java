package com.buddyfantasy.rating.application.usecases.getmatchratings;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.rating.domain.MyRatings;
import com.buddyfantasy.rating.domain.RatingStatus;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.rating.domain.SheetPlayer;
import com.buddyfantasy.rating.domain.Vote;

/**
 * A member's view of the ratings of a match at the server's {@code now} (RF-18 to RF-21): the players of the sheet,
 * goalkeepers first, with the member's own vote and, once the job closed the window, the published result. The
 * status is {@code CLOSED} from {@code closesAt}, but the results only appear when the job closes it.
 * {@code excluded} says whether the admin cancelled the member's ratings (RF-41), without a reason;
 * {@code excludedUserIds}, every member excluded from the match, is only for the admin and null for the rest.
 */
public record MatchRatingsView(UUID matchId, int matchday, Instant kickoffAt, RatingStatus status, Instant closesAt,
                               RatingFormat format, boolean excluded, List<UUID> excludedUserIds,
                               List<PlayerRatingView> players) {

    /** Goalkeepers first, as the squad. */
    private static final Comparator<SheetPlayer> SHEET_ORDER = Comparator.comparing(SheetPlayer::position)
            .thenComparing(SheetPlayer::playerId);

    /** {@code excludedUserIds} are all the members excluded from the match; only shown to the {@code admin}. */
    public static MatchRatingsView from(RatingWindow window, MyRatings mine, List<UUID> excludedUserIds,
            boolean admin, Instant now) {
        Map<UUID, Integer> myValues = mine.votes().stream()
                .collect(Collectors.toUnmodifiableMap(Vote::playerId, Vote::value));
        return new MatchRatingsView(window.matchId(), window.matchday(), window.kickoffAt(), window.status(now),
                window.closesAt(), window.rules().format(), excludedUserIds.contains(mine.userId()),
                admin ? List.copyOf(excludedUserIds) : null,
                window.players().stream()
                        .sorted(SHEET_ORDER)
                        .map(player -> PlayerRatingView.of(player, myValues.get(player.playerId()),
                                window.results().get(player.playerId())))
                        .toList());
    }
}
