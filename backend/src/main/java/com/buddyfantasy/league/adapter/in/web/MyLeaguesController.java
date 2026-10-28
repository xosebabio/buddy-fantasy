package com.buddyfantasy.league.adapter.in.web;

import java.util.List;

import com.buddyfantasy.league.application.usecases.listmyleagues.ListMyLeaguesQuery;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ligas")
@RestController
@RequiredArgsConstructor
class MyLeaguesController {

    private final CommandBus bus;

    @Operation(summary = "Ver mis ligas",
            description = "Las ligas de las que el usuario es miembro, con su rol en cada una. Vacía si no está en "
                    + "ninguna.")
    @ApiResponse(responseCode = "200", description = "Las ligas del usuario.")
    @GetMapping("/api/me/leagues")
    List<MyLeagueResponse> myLeagues(@AuthenticationPrincipal CurrentUser user) {
        return bus.ask(new ListMyLeaguesQuery(user.userId())).stream().map(MyLeagueResponse::from).toList();
    }
}
