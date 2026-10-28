package com.buddyfantasy.league.adapter.in.web;

import java.util.List;

import com.buddyfantasy.league.application.usecases.listmyleagues.ListMyLeaguesQuery;
import com.buddyfantasy.league.application.usecases.listmyleagues.MyLeagueView;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class MyLeaguesController {

    private final CommandBus bus;

    @GetMapping("/api/me/leagues")
    List<MyLeagueView> myLeagues(@AuthenticationPrincipal CurrentUser user) {
        return bus.ask(new ListMyLeaguesQuery(user.userId()));
    }
}
