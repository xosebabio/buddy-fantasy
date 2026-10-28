package com.buddyfantasy.league.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.usecases.listmyleagues.MyLeagueView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liga de la que el usuario es miembro.")
record MyLeagueResponse(
        @Schema(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
        UUID leagueId,
        @Schema(description = "Nombre de la liga.", example = "Peña Os Amigos")
        String name,
        @Schema(description = "Rol del usuario en la liga. `ADMIN` también juega como manager.", example = "ADMIN")
        LeagueRole role) {

    static MyLeagueResponse from(MyLeagueView view) {
        return new MyLeagueResponse(view.leagueId(), view.name(), view.role());
    }
}
