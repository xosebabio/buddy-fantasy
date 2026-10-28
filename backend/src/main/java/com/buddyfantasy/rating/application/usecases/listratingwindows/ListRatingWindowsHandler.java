package com.buddyfantasy.rating.application.usecases.listratingwindows;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.rating.application.port.out.RatingWindowsReadModel;
import com.buddyfantasy.rating.domain.RatingStatus;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The order depends on the server's clock, so it is applied here and not in the query. */
@Component
@RequiredArgsConstructor
class ListRatingWindowsHandler implements QueryHandler<ListRatingWindowsQuery, List<RatingWindowView>> {

    /** Open first (OPEN is declared before CLOSED), then the most recent kickoff first. */
    private static final Comparator<RatingWindowView> LISTING_ORDER = Comparator.comparing(RatingWindowView::status)
            .thenComparing(RatingWindowView::kickoffAt, Comparator.reverseOrder())
            .thenComparing(RatingWindowView::matchId);

    private final LeagueAccess access;
    private final RatingWindowsReadModel readModel;
    private final Clock clock;

    @Override
    public List<RatingWindowView> handle(ListRatingWindowsQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        Instant now = clock.instant();
        return readModel.findByLeagueId(query.leagueId(), query.userId()).stream()
                .map(window -> new RatingWindowView(window.matchId(), window.matchday(), window.kickoffAt(),
                        RatingStatus.of(window.closesAt(), window.closedAt(), now), window.closesAt(),
                        window.players(), window.myRatings()))
                .sorted(LISTING_ORDER)
                .toList();
    }
}
