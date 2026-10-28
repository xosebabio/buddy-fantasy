package com.buddyfantasy.league.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.usecases.listleaguemembers.MemberView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Miembro de una liga. El administrador también aparece, porque juega como manager.")
record MemberResponse(
        @Schema(description = "Id del usuario.", example = ApiExamples.USER_ID)
        UUID userId,
        @Schema(description = "Nombre que se muestra; null si no se encuentra su usuario.", nullable = true,
                example = "Iago Fernández")
        String displayName,
        @Schema(description = "Foto de su cuenta de Google; null si no tiene o no se encuentra su usuario.", nullable = true,
                example = "https://lh3.googleusercontent.com/a/iago")
        String avatarUrl,
        @Schema(description = "Rol en la liga.", example = "MANAGER")
        LeagueRole role,
        @Schema(description = "Cuándo se unió (o creó la liga, el administrador).", example = ApiExamples.INSTANT)
        Instant joinedAt) {

    static MemberResponse from(MemberView view) {
        return new MemberResponse(view.userId(), view.displayName(), view.avatarUrl(), view.role(), view.joinedAt());
    }
}
