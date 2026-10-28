package com.buddyfantasy.identity.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.identity.application.usecases.getcurrentuser.UserView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario con la sesión iniciada, con los datos de su cuenta de Google.")
record UserResponse(
        @Schema(description = "Id del usuario.", example = ApiExamples.USER_ID)
        UUID id,
        @Schema(description = "Nombre que se muestra a los demás.", example = "Iago Fernández")
        String displayName,
        @Schema(description = "Email de la cuenta de Google.", example = "iago@example.com")
        String email,
        @Schema(description = "Foto de la cuenta de Google; null si no tiene.", nullable = true,
                example = "https://lh3.googleusercontent.com/a/iago")
        String avatarUrl) {

    static UserResponse from(UserView view) {
        return new UserResponse(view.id(), view.displayName(), view.email(), view.avatarUrl());
    }
}
