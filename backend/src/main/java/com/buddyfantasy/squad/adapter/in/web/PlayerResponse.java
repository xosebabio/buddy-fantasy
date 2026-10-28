package com.buddyfantasy.squad.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.web.ApiExamples;
import com.buddyfantasy.squad.application.usecases.getplayer.PlayerView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ficha de un jugador del equipo real (RF-11). Importes en millones.")
record PlayerResponse(
        @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
        UUID id,
        @Schema(description = "Nombre.", example = "Iago Aspas")
        String name,
        @Schema(description = "Dorsal; null si no tiene.", nullable = true, example = "10")
        Integer shirtNumber,
        @Schema(description = "Posición en el campo.", example = "FORWARD")
        Position position,
        @Schema(description = "URL pública de la foto; null si no tiene.", nullable = true,
                example = "https://cdn.example.com/leagues/" + ApiExamples.LEAGUE_ID + "/players/"
                        + ApiExamples.PLAYER_ID + "/0b6f2c1a-5d4e-4f3a-9b8c-7d6e5f4a3b2c.webp")
        String photoUrl,
        @Schema(description = "Valor con el que se dio de alta.", example = "12.5")
        BigDecimal initialValue,
        @Schema(description = "Valor de mercado vigente; igual al inicial hasta que exista el mercado (Fase 3).",
                example = "12.5")
        BigDecimal currentValue,
        @Schema(description = "Cuándo se dio de alta.", example = ApiExamples.INSTANT)
        Instant createdAt,
        @Schema(description = "Cuándo se dio de baja; null si está activo.", nullable = true,
                example = ApiExamples.LATER_INSTANT)
        Instant retiredAt) {

    static PlayerResponse from(PlayerView view) {
        return new PlayerResponse(view.id(), view.name(), view.shirtNumber(), view.position(), view.photoUrl(),
                view.initialValue(), view.currentValue(), view.createdAt(), view.retiredAt());
    }
}
