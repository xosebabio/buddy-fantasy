package com.buddyfantasy.league.adapter.in.web;

import com.buddyfantasy.league.application.usecases.previewinvitation.InvitationPreview;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lo que muestra la pantalla «¿Unirte a la liga X?» antes de aceptar la invitación.")
record InvitationPreviewResponse(
        @Schema(description = "Nombre de la liga.", example = "Peña Os Amigos")
        String leagueName,
        @Schema(description = "Si el usuario ya es miembro de la liga.", example = "false")
        boolean alreadyMember) {

    static InvitationPreviewResponse from(InvitationPreview preview) {
        return new InvitationPreviewResponse(preview.leagueName(), preview.alreadyMember());
    }
}
