package com.buddyfantasy.match.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.usecases.getmatch.GetMatchQuery;
import com.buddyfantasy.match.application.usecases.getmatch.MatchView;
import com.buddyfantasy.match.application.usecases.listmatches.ListMatchesQuery;
import com.buddyfantasy.match.application.usecases.listmatches.MatchSummaryView;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MatchesController {

    private final CommandBus bus;

    MatchesController(CommandBus bus) {
        this.bus = bus;
    }

    @PostMapping("/api/leagues/{id}/matches")
    ResponseEntity<MatchView> schedule(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                       @Valid @RequestBody MatchRequest request) {
        MatchId matchId = bus.dispatch(new ScheduleMatchCommand(user.userId(), id, request.matchday(),
                request.opponent(), request.venue(), request.kickoffAt()));
        return ResponseEntity.created(URI.create("/api/leagues/" + id + "/matches/" + matchId.value()))
                .body(get(user, id, matchId.value()));
    }

    @GetMapping("/api/leagues/{id}/matches")
    List<MatchSummaryView> list(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new ListMatchesQuery(user.userId(), id));
    }

    @GetMapping("/api/leagues/{id}/matches/{matchId}")
    MatchView get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID matchId) {
        return bus.ask(new GetMatchQuery(user.userId(), id, matchId));
    }
}
