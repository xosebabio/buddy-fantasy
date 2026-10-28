package com.buddyfantasy.squad.adapter.in.web;

import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Foto ya subida que pasa a ser la del jugador.")
record ConfirmPhotoRequest(
        @Schema(description = "La `photoKey` que devolvió la petición de subida, de este jugador.",
                example = "leagues/" + ApiExamples.LEAGUE_ID + "/players/" + ApiExamples.PLAYER_ID
                        + "/0b6f2c1a-5d4e-4f3a-9b8c-7d6e5f4a3b2c.webp")
        @NotBlank String photoKey) {
}
