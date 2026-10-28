package com.buddyfantasy.rating.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.usecases.getmatchratings.GetMatchRatingsQuery;
import com.buddyfantasy.rating.application.usecases.listratingwindows.ListRatingWindowsQuery;
import com.buddyfantasy.rating.application.usecases.savemyratings.SaveMyRatingsCommand;
import com.buddyfantasy.rating.domain.Vote;
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

/** The rating windows of a league and the current user's own votes in each match (RF-17 to RF-21). */
@Tag(name = "Valoraciones", description = """
        Las ventanas de valoración de la liga y los votos del usuario en cada partido (RF-17 a RF-21). Se abren al \
        cerrar el partido y son anónimas: ninguna respuesta incluye votos de otro miembro, tampoco para el \
        administrador.""")
@RestController
@RequiredArgsConstructor
class RatingsController {

    private static final String MATCH_NOT_CLOSED = """
            {"type": "about:blank", "title": "Conflict", "status": 409,
             "detail": "Match 5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f is not closed yet", "code": "MATCH_NOT_CLOSED",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/ratings/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""";

    private final CommandBus bus;

    @Operation(summary = "Ver las ventanas de valoración",
            description = "Rol mínimo: miembro. Una por partido cerrado: primero las abiertas y después del partido "
                    + "más reciente al más antiguo, con cuántos jugadores ha valorado el usuario.")
    @ApiResponse(responseCode = "200", description = "Las ventanas de la liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/ratings")
    List<RatingWindowResponse> list(@AuthenticationPrincipal CurrentUser user,
                                    @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                    @PathVariable UUID id) {
        return bus.ask(new ListRatingWindowsQuery(user.userId(), id)).stream().map(RatingWindowResponse::from).toList();
    }

    @Operation(summary = "Ver las valoraciones de un partido",
            description = "Rol mínimo: miembro. Los jugadores del acta con el voto del usuario y, cuando se cierra la "
                    + "ventana, los votos que contaron, la media y los puntos de cada uno (RF-21).")
    @ApiResponse(responseCode = "200", description = "Las valoraciones del partido.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = "El partido aún no está cerrado, así que no tiene ventana "
            + "(`code`: `MATCH_NOT_CLOSED`).", content = @Content(examples = @ExampleObject(value = MATCH_NOT_CLOSED)))
    @GetMapping("/api/leagues/{id}/ratings/{matchId}")
    MatchRatingsResponse get(@AuthenticationPrincipal CurrentUser user,
                             @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                             @PathVariable UUID id,
                             @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                             @PathVariable UUID matchId) {
        return MatchRatingsResponse.from(bus.ask(new GetMatchRatingsQuery(user.userId(), id, matchId)));
    }

    @Operation(summary = "Guardar mis valoraciones de un partido",
            description = """
                    Rol mínimo: miembro (también el administrador). Reemplaza todos los votos del usuario en el \
                    partido mientras la ventana siga abierta según el reloj del servidor (RF-18 a RF-20): un voto \
                    que llega a la vez que el cierre queda dentro o recibe `409`. Valorar es opcional.""")
    @ApiResponse(responseCode = "200", description = "Las valoraciones del partido con los votos guardados.")
    @ApiResponse(responseCode = "400", description = """
            Algún voto no es válido; `code` dice por qué: `INVALID_RATING` (fuera del formato de la ventana), \
            `PLAYER_NOT_IN_SHEET` (el jugador no jugó) o `DUPLICATE_PLAYER` (dos votos al mismo jugador).""",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "A rating in SPADES is between 0 and 4", "code": "INVALID_RATING",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/ratings/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""")))
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El partido no es de la liga.")
    @ApiResponse(responseCode = "409", description = """
            No se puede votar; `code` dice por qué: `RATING_WINDOW_CLOSED` (la ventana se cerró o ya pasó \
            `closesAt`) o `MATCH_NOT_CLOSED` (el partido aún no tiene ventana).""",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Conflict", "status": 409,
                     "detail": "The ratings of match 5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f closed at 2026-01-12T12:00:00Z",
                     "code": "RATING_WINDOW_CLOSED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/ratings/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""")))
    @PutMapping("/api/leagues/{id}/ratings/{matchId}")
    MatchRatingsResponse save(@AuthenticationPrincipal CurrentUser user,
                              @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                              @PathVariable UUID id,
                              @Parameter(description = "Id del partido.", example = ApiExamples.MATCH_ID)
                              @PathVariable UUID matchId,
                              @Valid @RequestBody RatingsRequest request) {
        return MatchRatingsResponse.from(bus.dispatch(new SaveMyRatingsCommand(user.userId(), id, matchId,
                request.ratings().stream()
                        .map(rating -> new Vote(rating.playerId(), rating.value()))
                        .toList())));
    }
}
