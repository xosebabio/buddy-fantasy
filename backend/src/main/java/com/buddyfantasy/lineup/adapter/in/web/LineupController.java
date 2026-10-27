package com.buddyfantasy.lineup.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.lineup.application.usecases.getlockedlineups.GetLockedLineupsQuery;
import com.buddyfantasy.lineup.application.usecases.getlockedlineups.LockedLineupsView;
import com.buddyfantasy.lineup.application.usecases.getmylineup.GetMyLineupQuery;
import com.buddyfantasy.lineup.application.usecases.getmylineup.MyLineupView;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The current user's own working lineup in a league, and every member's lineup once a match is locked. */
@RestController
class LineupController {

    private final CommandBus bus;

    LineupController(CommandBus bus) {
        this.bus = bus;
    }

    @GetMapping("/api/leagues/{id}/lineup")
    MyLineupView get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new GetMyLineupQuery(user.userId(), id));
    }

    @PutMapping("/api/leagues/{id}/lineup")
    MyLineupView save(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                      @Valid @RequestBody LineupRequest request) {
        return bus.dispatch(new SaveLineupCommand(user.userId(), id, request.playerIds()));
    }

    @GetMapping("/api/leagues/{id}/lineups/{matchId}")
    LockedLineupsView locked(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                             @PathVariable UUID matchId) {
        return bus.ask(new GetLockedLineupsQuery(user.userId(), id, matchId));
    }
}
