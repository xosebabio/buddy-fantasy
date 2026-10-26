package com.buddyfantasy.league.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.getleague.GetLeagueQuery;
import com.buddyfantasy.league.application.usecases.getleague.LeagueView;
import com.buddyfantasy.league.application.usecases.getleaguesettings.GetLeagueSettingsQuery;
import com.buddyfantasy.league.application.usecases.updateleaguesettings.UpdateLeagueSettingsCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(user.userId(), request.name()));
        return ResponseEntity.created(URI.create("/api/leagues/" + created.id().value()))
                .body(new CreateLeagueResponse(created.id().value(), created.invitationToken()));
    }

    @GetMapping("/api/leagues/{id}")
    LeagueView get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new GetLeagueQuery(user.userId(), id));
    }

    @GetMapping("/api/leagues/{id}/settings")
    LeagueSettingsBody settings(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return LeagueSettingsWebMapper.toBody(bus.ask(new GetLeagueSettingsQuery(user.userId(), id)));
    }

    @PutMapping("/api/leagues/{id}/settings")
    LeagueSettingsBody updateSettings(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                      @RequestBody LeagueSettingsBody body) {
        bus.dispatch(new UpdateLeagueSettingsCommand(user.userId(), id,
                LeagueSettingsWebMapper.toSettings(body), LeagueSettingsWebMapper.toScoringRules(body)));
        return settings(user, id);
    }
}
