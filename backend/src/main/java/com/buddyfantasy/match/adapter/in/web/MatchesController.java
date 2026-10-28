package com.buddyfantasy.match.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.usecases.closematch.CloseMatchCommand;
import com.buddyfantasy.match.application.usecases.deletematch.DeleteMatchCommand;
import com.buddyfantasy.match.application.usecases.getmatch.GetMatchQuery;
import com.buddyfantasy.match.application.usecases.listmatchaudit.ListMatchAuditQuery;
import com.buddyfantasy.match.application.usecases.listmatches.ListMatchesQuery;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.RecordMatchSheetCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.application.usecases.updatematch.UpdateMatchCommand;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.MatchSheetWarning;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Partidos", description = """
        El calendario del equipo real, el acta de cada partido y su cierre (RF-12 a RF-16). Cada cambio del \
        administrador queda en la auditoría del partido (RNF-04).""")
@RestController
@RequiredArgsConstructor
class MatchesController {

    private static final String MATCHDAY_TAKEN = """
            {"type": "about:blank", "title": "Conflict", "status": 409, "detail": "Matchday already has a match",
             "code": "MATCHDAY_TAKEN", "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches"}""";
    private static final String INVALID_MATCH = """
            {"type": "about:blank", "title": "Bad Request", "status": 400, "detail": "Matchday must be >= 1",
             "code": "INVALID_MATCHDAY", "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches"}""";

    private final CommandBus bus;

    @Operation(summary = "Programar un partido",
            description = "Rol mínimo: administrador. Añade un partido al calendario, en estado `SCHEDULED` "
                    + "(RF-12). Una jornada tiene un único partido.")
    @ApiResponse(responseCode = "201", description = "Partido programado. La cabecera `Location` apunta a él.",
            headers = @Header(name = "Location", description = "Ruta del partido creado.",
                    schema = @Schema(type = "string",
                            example = "/api/leagues/" + ApiExamples.LEAGUE_ID + "/matches/" + ApiExamples.MATCH_ID)))
    @ApiResponse(responseCode = "400", description = "Jornada o rival fuera de sus límites; `code` dice cuál: "
            + "`INVALID_MATCHDAY` (menor que `match.limits.matchday-min`) o `INVALID_OPPONENT` (más largo "
            + "que `match.limits.opponent-max-length`).",
            content = @Content(examples = @ExampleObject(value = INVALID_MATCH)))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "409", description = "La jornada ya tiene partido (`code`: `MATCHDAY_TAKEN`), u otro "
            + "partido empieza a la misma hora (`KICKOFF_TAKEN`).",
            content = @Content(examples = @ExampleObject(value = MATCHDAY_TAKEN)))
    @PostMapping("/api/leagues/{id}/matches")
    ResponseEntity<MatchResponse> schedule(@AuthenticationPrincipal CurrentUser user,
                                           @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                           @PathVariable UUID id,
                                           @Valid @RequestBody MatchRequest request) {
        MatchId matchId = bus.dispatch(new ScheduleMatchCommand(user.userId(), id, request.matchday(),
                request.opponent(), request.venue(), request.kickoffAt()));
        return ResponseEntity.created(URI.create("/api/leagues/" + id + "/matches/" + matchId.value()))
                .body(get(user, id, matchId.value()));
    }

    @Operation(summary = "Ver el calendario",
            description = "Rol mínimo: miembro. Todos los partidos de la liga por jornada. Un manager solo ve el "
                    + "resultado de los partidos cerrados.")
    @ApiResponse(responseCode = "200", description = "Los partidos de la liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/matches")
    List<MatchSummaryResponse> list(@AuthenticationPrincipal CurrentUser user,
                                    @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                    @PathVariable UUID id) {
        return bus.ask(new ListMatchesQuery(user.userId(), id)).stream().map(MatchSummaryResponse::from).toList();
    }

    @Operation(summary = "Ver un partido",
            description = "Rol mínimo: miembro. Un manager no ve el acta hasta que el partido se cierra; el "
                    + "administrador la ve siempre, con sus avisos de coherencia.")
    @ApiResponse(responseCode = "200", description = "El partido.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @GetMapping("/api/leagues/{id}/matches/{matchId}")
    MatchResponse get(@AuthenticationPrincipal CurrentUser user,
                      @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                      @PathVariable UUID id,
                      @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                      @PathVariable UUID matchId) {
        return MatchResponse.from(bus.ask(new GetMatchQuery(user.userId(), id, matchId)));
    }

    @Operation(summary = "Editar o aplazar un partido",
            description = """
                    Rol mínimo: administrador. Reemplaza jornada, rival, campo y hora. Cambiar la jornada o la hora \
                    es un aplazamiento (RF-12): si las alineaciones ya estaban bloqueadas y la nueva hora de bloqueo \
                    aún no ha llegado, se desbloquean. En un partido \
                    cerrado solo se pueden cambiar rival y campo.""")
    @ApiResponse(responseCode = "200", description = "El partido editado.")
    @ApiResponse(responseCode = "400", description = "Jornada o rival fuera de sus límites; `code` dice cuál: "
            + "`INVALID_MATCHDAY` (menor que `match.limits.matchday-min`) o `INVALID_OPPONENT` (más largo "
            + "que `match.limits.opponent-max-length`).",
            content = @Content(examples = @ExampleObject(value = INVALID_MATCH)))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "La jornada ya tiene partido (`code`: `MATCHDAY_TAKEN`), otro "
            + "partido empieza a la misma hora (`KICKOFF_TAKEN`), o se cambia la jornada o la hora de un partido "
            + "cerrado (`MATCH_CLOSED`).",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "The matchday and kickoff of a closed match cannot change", "code": "MATCH_CLOSED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""")))
    @PutMapping("/api/leagues/{id}/matches/{matchId}")
    MatchResponse update(@AuthenticationPrincipal CurrentUser user,
                         @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                         @PathVariable UUID id,
                         @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                         @PathVariable UUID matchId,
                         @Valid @RequestBody MatchRequest request) {
        bus.dispatch(new UpdateMatchCommand(user.userId(), id, matchId, request.matchday(), request.opponent(),
                request.venue(), request.kickoffAt()));
        return get(user, id, matchId);
    }

    @Operation(summary = "Borrar un partido",
            description = "Rol mínimo: administrador. Solo un partido sin cerrar, por ejemplo uno suspendido que no "
                    + "se va a jugar: no hay estado «cancelado».")
    @ApiResponse(responseCode = "204", description = "Partido borrado.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "El partido está cerrado (`code`: `MATCH_CLOSED`).",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "A closed match cannot be deleted", "code": "MATCH_CLOSED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""")))
    @DeleteMapping("/api/leagues/{id}/matches/{matchId}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user,
                                @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                @PathVariable UUID id,
                                @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                                @PathVariable UUID matchId) {
        bus.dispatch(new DeleteMatchCommand(user.userId(), id, matchId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Guardar el acta",
            description = """
                    Rol mínimo: administrador. Reemplaza el acta entera: resultado y estadísticas de los jugadores \
                    que jugaron (RF-13, RF-14). Antes del cierre es un borrador que puede ir sin resultado. Después \
                    es una corrección (RF-16): debe seguir completa, recalcula los puntos y los jugadores que se \
                    quitan pierden sus valoraciones. Guardar la misma acta no hace nada.""")
    @ApiResponse(responseCode = "200", description = "Acta guardada, con sus avisos de coherencia.")
    @ApiResponse(responseCode = "400", description = """
            El acta no es válida; `code` dice por qué: `INVALID_GOALS` (más que `match.limits.goals-max`), \
            `INVALID_STAT_COUNT` (alguna estadística negativa o más que `match.limits.stat-count-max`), \
            `INVALID_CARDS` (doble amarilla junto a otra tarjeta), `SHEET_TOO_LARGE` (más de \
            `match.limits.sheet-players-max` jugadores), `DUPLICATE_PLAYER`, `UNKNOWN_PLAYER` (no es de la \
            plantilla) o `INACTIVE_PLAYER` (dado de baja, si no estaba ya en el acta). Un resultado a medias no \
            lleva `code`: es un campo que falta (`violations`).""",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "A double yellow excludes the yellow card and the straight red", "code": "INVALID_CARDS",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f/sheet"}""")))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "Corrección de un partido cerrado sin resultado o sin jugadores "
            + "(`code`: `MATCH_SHEET_INCOMPLETE`).", content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "A closed match needs a result and at least one player",
                     "code": "MATCH_SHEET_INCOMPLETE",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f/sheet"}""")))
    @PutMapping("/api/leagues/{id}/matches/{matchId}/sheet")
    MatchSheetResponse recordSheet(@AuthenticationPrincipal CurrentUser user,
                                   @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                   @PathVariable UUID id,
                                   @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                                   @PathVariable UUID matchId,
                                   @Valid @RequestBody MatchSheetRequest request) {
        MatchResultRequest result = request.result();
        List<MatchSheetWarning> warnings = bus.dispatch(new RecordMatchSheetCommand(user.userId(), id, matchId,
                result == null ? null : result.goalsFor(), result == null ? null : result.goalsAgainst(),
                request.performances().stream().map(PlayerPerformanceRequest::toLine).toList()));
        return new MatchSheetResponse(warnings);
    }

    @Operation(summary = "Cerrar un partido",
            description = """
                    Rol mínimo: administrador. Desde la hora de inicio y con el acta completa (resultado y al menos \
                    un jugador). Bloquea las alineaciones si aún no lo estaban, abre la ventana de valoración y \
                    puntúa la jornada (RF-15, RF-17). Cerrarlo otra vez no hace nada.""")
    @ApiResponse(responseCode = "200", description = "El partido cerrado.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "Aún no ha empezado (`code`: `MATCH_NOT_STARTED`), o al acta "
            + "le falta el resultado o los jugadores (`MATCH_SHEET_INCOMPLETE`).",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "A match cannot be closed before kickoff", "code": "MATCH_NOT_STARTED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f/close"}""")))
    @PostMapping("/api/leagues/{id}/matches/{matchId}/close")
    MatchResponse close(@AuthenticationPrincipal CurrentUser user,
                        @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                        @PathVariable UUID id,
                        @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                        @PathVariable UUID matchId) {
        bus.dispatch(new CloseMatchCommand(user.userId(), id, matchId));
        return get(user, id, matchId);
    }

    @Operation(summary = "Ver la auditoría de un partido",
            description = "Rol mínimo: administrador. Cada cambio del administrador en el partido, del más reciente "
                    + "al más antiguo, con su estado antes y después (RNF-04).")
    @ApiResponse(responseCode = "200", description = "El historial del partido.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @GetMapping("/api/leagues/{id}/matches/{matchId}/audit")
    List<MatchAuditEntryResponse> audit(@AuthenticationPrincipal CurrentUser user,
                                        @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                        @PathVariable UUID id,
                                        @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                                        @PathVariable UUID matchId) {
        return bus.ask(new ListMatchAuditQuery(user.userId(), id, matchId)).stream()
                .map(MatchAuditEntryResponse::from)
                .toList();
    }
}
