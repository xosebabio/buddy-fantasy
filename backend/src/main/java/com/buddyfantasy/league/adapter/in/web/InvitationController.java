package com.buddyfantasy.league.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.league.application.usecases.previewinvitation.PreviewInvitationQuery;
import com.buddyfantasy.league.application.usecases.regenerateinvitation.RegenerateInvitationCommand;
import com.buddyfantasy.league.application.usecases.revokeinvitation.RevokeInvitationCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Invitation tokens travel in the path: never log request URLs under {@code /api/invitations/}. */
@Tag(name = "Invitaciones", description = """
        Enlace de invitación de una liga (RF-06): una liga tiene como mucho uno activo, y quien lo abre se une como \
        manager.""")
@RestController
@RequiredArgsConstructor
class InvitationController {

    private static final String TOKEN = "q7Z2mXk9LwP4vR1sT8yB3nC6dF0gH5jK";

    private final CommandBus bus;

    @Operation(summary = "Regenerar el enlace de invitación",
            description = "Rol mínimo: administrador. Crea un enlace nuevo y el anterior deja de funcionar.")
    @ApiResponse(responseCode = "200", description = "El enlace nuevo.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @PostMapping("/api/leagues/{id}/invitation")
    InvitationTokenResponse regenerate(@AuthenticationPrincipal CurrentUser user,
                                       @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                       @PathVariable UUID id) {
        return new InvitationTokenResponse(bus.dispatch(new RegenerateInvitationCommand(user.userId(), id)));
    }

    @Operation(summary = "Revocar el enlace de invitación",
            description = "Rol mínimo: administrador. El enlace activo deja de funcionar y nadie más puede unirse "
                    + "hasta que se regenere. Sin enlace activo no hace nada.")
    @ApiResponse(responseCode = "204", description = "Enlace revocado.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @DeleteMapping("/api/leagues/{id}/invitation")
    ResponseEntity<Void> revoke(@AuthenticationPrincipal CurrentUser user,
                                @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                @PathVariable UUID id) {
        bus.dispatch(new RevokeInvitationCommand(user.userId(), id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ver a qué liga invita un enlace",
            description = "Cualquier usuario con sesión. Para la pantalla «¿Unirte a la liga X?»: solo el nombre de "
                    + "la liga y si ya es miembro.")
    @ApiResponse(responseCode = "200", description = "La liga del enlace.")
    @ApiResponse(responseCode = "404", description = "El enlace no existe o fue revocado.")
    @GetMapping("/api/invitations/{token}")
    InvitationPreviewResponse preview(@AuthenticationPrincipal CurrentUser user,
                                      @Parameter(description = "Token del enlace de invitación.", example = TOKEN)
                                      @PathVariable String token) {
        return InvitationPreviewResponse.from(bus.ask(new PreviewInvitationQuery(user.userId(), token)));
    }

    @Operation(summary = "Unirse a una liga",
            description = "Cualquier usuario con sesión. Se une como manager. Si ya era miembro, conserva su rol y su "
                    + "fecha de alta: se puede repetir sin efectos.")
    @ApiResponse(responseCode = "200", description = "La liga a la que se ha unido.")
    @ApiResponse(responseCode = "404", description = "El enlace no existe o fue revocado.")
    @PostMapping("/api/invitations/{token}/join")
    JoinLeagueResponse join(@AuthenticationPrincipal CurrentUser user,
                            @Parameter(description = "Token del enlace de invitación.", example = TOKEN)
                            @PathVariable String token) {
        return new JoinLeagueResponse(bus.dispatch(new JoinLeagueCommand(user.userId(), token)).value());
    }
}
