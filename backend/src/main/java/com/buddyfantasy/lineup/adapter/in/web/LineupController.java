package com.buddyfantasy.lineup.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.lineup.application.usecases.getlockedlineups.GetLockedLineupsQuery;
import com.buddyfantasy.lineup.application.usecases.getmylineup.GetMyLineupQuery;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The current user's own working lineup in a league, and every member's lineup once a match is locked. */
@Tag(name = "Alineaciones", description = """
        La alineación de trabajo de cada manager y las alineaciones bloqueadas de cada partido (RF-24 a RF-29).""")
@RestController
@RequiredArgsConstructor
class LineupController {

    private final CommandBus bus;

    @Operation(summary = "Ver mi alineación",
            description = "Rol mínimo: miembro. La alineación de trabajo del usuario, el partido para el que cuenta, "
                    + "los cambios que le quedan y sus avisos.")
    @ApiResponse(responseCode = "200", description = "La alineación de trabajo.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/lineup")
    MyLineupResponse get(@AuthenticationPrincipal CurrentUser user,
                         @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                         @PathVariable UUID id) {
        return MyLineupResponse.from(bus.ask(new GetMyLineupQuery(user.userId(), id)));
    }

    @Operation(summary = "Guardar mi alineación",
            description = """
                    Rol mínimo: miembro. Reemplaza la alineación de trabajo, que cuenta para el próximo partido \
                    cuya hora de bloqueo no ha pasado según el reloj del servidor (RF-24, RF-25). Los cambios se \
                    cuentan contra la alineación bloqueada del último partido; sustituir a un jugador dado de baja \
                    es gratis (RF-26). Si no la vuelve a tocar, se arrastra a los partidos siguientes (RF-28). Guardar \
                    la misma alineación no hace nada.""")
    @ApiResponse(responseCode = "200", description = "La alineación guardada.")
    @ApiResponse(responseCode = "400", description = """
            La alineación no es válida; `code` dice por qué: `SIZE` (no tiene `lineupSize` jugadores), \
            `DUPLICATE_PLAYER`, `UNKNOWN_PLAYER` (no es de la plantilla), `INACTIVE_PLAYER` (dado de baja), \
            `FORMATION` (no llega a los mínimos por posición) u `OVER_BUDGET` (supera el presupuesto).""",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "A lineup has exactly 7 players", "code": "SIZE",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/lineup"}""")))
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "409", description = "Supera los cambios permitidos por jornada "
            + "(`code`: `MAX_CHANGES_EXCEEDED`).", content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "The lineup has 3 changes and at most 2 are allowed per matchday", "code": "MAX_CHANGES_EXCEEDED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/lineup"}""")))
    @PutMapping("/api/leagues/{id}/lineup")
    MyLineupResponse save(@AuthenticationPrincipal CurrentUser user,
                          @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                          @PathVariable UUID id,
                          @Valid @RequestBody LineupRequest request) {
        return MyLineupResponse.from(bus.dispatch(new SaveLineupCommand(user.userId(), id, request.playerIds())));
    }

    @Operation(summary = "Ver las alineaciones bloqueadas de un partido",
            description = "Rol mínimo: miembro. Las de todos los managers, pero solo una vez bloqueadas: antes nadie "
                    + "ve las de los demás. La bloqueada es la que puntúa (RF-27).")
    @ApiResponse(responseCode = "200", description = "Las alineaciones bloqueadas.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "Las alineaciones del partido aún no están bloqueadas "
            + "(`code`: `LINEUPS_NOT_LOCKED`).", content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "The lineups of match 5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f are not locked yet",
                     "code": "LINEUPS_NOT_LOCKED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/lineups/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""")))
    @GetMapping("/api/leagues/{id}/lineups/{matchId}")
    LockedLineupsResponse locked(@AuthenticationPrincipal CurrentUser user,
                                 @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                 @PathVariable UUID id,
                                 @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                                 @PathVariable UUID matchId) {
        return LockedLineupsResponse.from(bus.ask(new GetLockedLineupsQuery(user.userId(), id, matchId)));
    }
}
