package com.buddyfantasy.rating.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.usecases.getmatchratings.GetMatchRatingsQuery;
import com.buddyfantasy.rating.application.usecases.getmatchratings.MatchRatingsView;
import com.buddyfantasy.rating.application.usecases.listratingwindows.ListRatingWindowsQuery;
import com.buddyfantasy.rating.application.usecases.listratingwindows.RatingWindowView;
import com.buddyfantasy.rating.application.usecases.savemyratings.SaveMyRatingsCommand;
import com.buddyfantasy.rating.domain.Vote;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The rating windows of a league and the current user's own votes in each match (RF-17 to RF-21). */
@RestController
@RequiredArgsConstructor
class RatingsController {

    private final CommandBus bus;

    @GetMapping("/api/leagues/{id}/ratings")
    List<RatingWindowView> list(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new ListRatingWindowsQuery(user.userId(), id));
    }

    @GetMapping("/api/leagues/{id}/ratings/{matchId}")
    MatchRatingsView get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                         @PathVariable UUID matchId) {
        return bus.ask(new GetMatchRatingsQuery(user.userId(), id, matchId));
    }

    @PutMapping("/api/leagues/{id}/ratings/{matchId}")
    MatchRatingsView save(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                          @PathVariable UUID matchId, @Valid @RequestBody RatingsRequest request) {
        return bus.dispatch(new SaveMyRatingsCommand(user.userId(), id, matchId, request.ratings().stream()
                .map(rating -> new Vote(rating.playerId(), rating.value()))
                .toList()));
    }
}
