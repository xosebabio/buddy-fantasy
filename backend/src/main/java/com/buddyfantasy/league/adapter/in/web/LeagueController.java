package com.buddyfantasy.league.adapter.in.web;

import java.net.URI;

import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.domain.LeagueId;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LeagueController {

    private final CommandBus bus;

    LeagueController(CommandBus bus) {
        this.bus = bus;
    }

    @PostMapping("/api/leagues")
    ResponseEntity<CreateLeagueResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                @Valid @RequestBody CreateLeagueRequest request) {
        LeagueId id = bus.dispatch(new CreateLeagueCommand(user.userId(), request.name()));
        return ResponseEntity.created(URI.create("/api/leagues/" + id.value()))
                .body(new CreateLeagueResponse(id.value()));
    }
}
