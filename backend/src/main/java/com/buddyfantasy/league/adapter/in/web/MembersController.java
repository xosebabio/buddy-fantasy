package com.buddyfantasy.league.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.league.application.usecases.leaveleague.LeaveLeagueCommand;
import com.buddyfantasy.league.application.usecases.listleaguemembers.ListLeagueMembersQuery;
import com.buddyfantasy.league.application.usecases.removemember.RemoveMemberCommand;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Miembros", description = "Los miembros de una liga: verlos, expulsarlos y abandonar la liga.")
@RestController
@RequiredArgsConstructor
class MembersController {

    private static final String ADMIN_NOT_REMOVABLE = """
            {"type": "about:blank", "title": "Conflict", "status": 409,
             "detail": "The league admin cannot be removed or leave the league",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/members/me"}""";

    private final CommandBus bus;

    @Operation(summary = "Ver los miembros de la liga",
            description = "Rol mínimo: miembro. Todos los miembros actuales, también el administrador.")
    @ApiResponse(responseCode = "200", description = "Los miembros de la liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/members")
    List<MemberResponse> list(@AuthenticationPrincipal CurrentUser user,
                              @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                              @PathVariable UUID id) {
        return bus.ask(new ListLeagueMembersQuery(user.userId(), id)).stream().map(MemberResponse::from).toList();
    }

    @Operation(summary = "Expulsar a un miembro",
            description = "Rol mínimo: administrador. El administrador no se puede expulsar a sí mismo.")
    @ApiResponse(responseCode = "204", description = "Miembro expulsado.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El usuario a expulsar no es miembro de la liga.")
    @ApiResponse(responseCode = "409", description = "Se intenta expulsar al administrador.",
            content = @Content(examples = @ExampleObject(value = ADMIN_NOT_REMOVABLE)))
    @DeleteMapping("/api/leagues/{id}/members/{memberId}")
    ResponseEntity<Void> remove(@AuthenticationPrincipal CurrentUser user,
                                @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                @PathVariable UUID id,
                                @Parameter(description = "Id del usuario a expulsar.",
                                        example = ApiExamples.OTHER_USER_ID)
                                @PathVariable UUID memberId) {
        bus.dispatch(new RemoveMemberCommand(user.userId(), id, memberId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Abandonar la liga",
            description = "Rol mínimo: miembro. El administrador no puede abandonar su liga.")
    @ApiResponse(responseCode = "204", description = "El usuario ya no es miembro de la liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "409", description = "El usuario es el administrador de la liga.",
            content = @Content(examples = @ExampleObject(value = ADMIN_NOT_REMOVABLE)))
    @DeleteMapping("/api/leagues/{id}/members/me")
    ResponseEntity<Void> leave(@AuthenticationPrincipal CurrentUser user,
                               @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                               @PathVariable UUID id) {
        bus.dispatch(new LeaveLeagueCommand(user.userId(), id));
        return ResponseEntity.noContent().build();
    }
}
