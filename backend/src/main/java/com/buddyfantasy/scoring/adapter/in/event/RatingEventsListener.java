package com.buddyfantasy.scoring.adapter.in.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.rating.PlayerRatingData;
import com.buddyfantasy.rating.RatingWindowClosed;
import com.buddyfantasy.rating.RatingsCorrected;
import com.buddyfantasy.scoring.application.usecases.applyratings.ApplyRatingsCommand;
import com.buddyfantasy.shared.application.CommandBus;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Keeps scoring's copy of the points for ratings. Delivery is at least once and every command is idempotent. */
@Component
@RequiredArgsConstructor
class RatingEventsListener {

    private final CommandBus bus;

    /** RF-22, RF-23: the last copy scoring needs to score the match. */
    @ApplicationModuleListener
    void on(RatingWindowClosed event) {
        bus.dispatch(new ApplyRatingsCommand(event.leagueId(), event.matchId(), ratingPoints(event.players()),
                event.occurredAt()));
    }

    /** RF-16: replaces the copy and recalculates the match with its frozen table. */
    @ApplicationModuleListener
    void on(RatingsCorrected event) {
        bus.dispatch(new ApplyRatingsCommand(event.leagueId(), event.matchId(), ratingPoints(event.players()),
                event.occurredAt()));
    }

    private static Map<UUID, Integer> ratingPoints(List<PlayerRatingData> players) {
        return players.stream().collect(Collectors.toMap(PlayerRatingData::playerId, PlayerRatingData::ratingPoints));
    }
}
