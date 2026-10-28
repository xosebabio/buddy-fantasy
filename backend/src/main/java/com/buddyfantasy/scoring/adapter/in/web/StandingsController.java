package com.buddyfantasy.scoring.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.application.usecases.getmatchdayscores.GetMatchdayScoresQuery;
import com.buddyfantasy.scoring.application.usecases.getstandings.GetStandingsQuery;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The overall standings of a league and those of each scored match (RF-32), for every member. Read only. */
@Tag(name = "Clasificación", description = "Clasificaciones e historiales de puntos (RF-30 a RF-33). Solo lectura.")
@RestController
@RequiredArgsConstructor
class StandingsController {

    private final CommandBus bus;

    @Operation(summary = "Ver la clasificación general",
            description = """
                    Rol mínimo: miembro. Los miembros actuales por puntos acumulados (RF-32); a cada uno le cuentan \
                    las jornadas que empezaron después de unirse a la liga. Los empatados comparten posición.""")
    @ApiResponse(responseCode = "200", description = "La clasificación general.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/standings")
    List<StandingResponse> overall(@AuthenticationPrincipal CurrentUser user,
                                   @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                   @PathVariable UUID id) {
        return bus.ask(new GetStandingsQuery(user.userId(), id)).stream().map(StandingResponse::from).toList();
    }

    @Operation(summary = "Ver los puntos de una jornada",
            description = "Rol mínimo: miembro. La clasificación de la jornada y los puntos de cada jugador del acta "
                    + "(RF-30 a RF-32). Hay puntos cuando el partido está cerrado, sus alineaciones bloqueadas y su "
                    + "ventana de valoración cerrada (RF-17): hasta entonces la jornada no cuenta en la "
                    + "clasificación.")
    @ApiResponse(responseCode = "200", description = "Los puntos de la jornada.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "El partido aún no tiene puntos: no está cerrado o su ventana de valoración sigue "
            + "abierta (`code`: `MATCH_NOT_SCORED`).",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "Match 5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f is not scored yet", "code": "MATCH_NOT_SCORED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/standings/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""")))
    @GetMapping("/api/leagues/{id}/standings/{matchId}")
    MatchdayScoresResponse matchday(@AuthenticationPrincipal CurrentUser user,
                                    @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                    @PathVariable UUID id,
                                    @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                                    @PathVariable UUID matchId) {
        return MatchdayScoresResponse.from(bus.ask(new GetMatchdayScoresQuery(user.userId(), id, matchId)));
    }
}
