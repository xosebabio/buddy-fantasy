package com.buddyfantasy.squad.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.squad.application.usecases.getplayer.GetPlayerQuery;
import com.buddyfantasy.squad.application.usecases.getplayer.PlayerView;
import com.buddyfantasy.squad.application.usecases.listplayers.ListPlayersQuery;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import com.buddyfantasy.squad.domain.PlayerId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PlayersController {

    private final CommandBus bus;

    PlayersController(CommandBus bus) {
        this.bus = bus;
    }

    @PostMapping("/api/leagues/{id}/players")
    ResponseEntity<PlayerView> register(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                        @Valid @RequestBody RegisterPlayerRequest request) {
        PlayerId playerId = bus.dispatch(new RegisterPlayerCommand(user.userId(), id, request.name(),
                request.shirtNumber(), request.position(), request.initialValue()));
        return ResponseEntity.created(URI.create("/api/leagues/" + id + "/players/" + playerId.value()))
                .body(get(user, id, playerId.value()));
    }

    @GetMapping("/api/leagues/{id}/players")
    List<PlayerView> list(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                          @RequestParam(defaultValue = "false") boolean includeRetired) {
        return bus.ask(new ListPlayersQuery(user.userId(), id, includeRetired));
    }

    @GetMapping("/api/leagues/{id}/players/{playerId}")
    PlayerView get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID playerId) {
        return bus.ask(new GetPlayerQuery(user.userId(), id, playerId));
    }
}
