package com.buddyfantasy.squad.adapter.in.web;

import java.time.Instant;
import java.util.Map;

import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

/** The browser sends a PUT to {@code uploadUrl} with these {@code headers}, then confirms {@code photoKey}. */
@Schema(description = """
        Subida prefirmada: el navegador envía la imagen con un `PUT` a `uploadUrl` con estas `headers` y después \
        confirma `photoKey`.""")
record PhotoUploadResponse(
        @Schema(description = "URL prefirmada del almacenamiento a la que subir la imagen.",
                example = "https://buddy-fantasy-photos.s3.eu-west-1.amazonaws.com/leagues/" + ApiExamples.LEAGUE_ID
                        + "/players/" + ApiExamples.PLAYER_ID + "/0b6f2c1a-5d4e-4f3a-9b8c-7d6e5f4a3b2c.webp"
                        + "?X-Amz-Signature=…")
        String uploadUrl,
        @Schema(description = "Cabeceras que el `PUT` debe enviar tal cual.",
                example = "{\"Content-Type\": \"image/webp\", "
                        + "\"Cache-Control\": \"public, max-age=31536000, immutable\"}")
        Map<String, String> headers,
        @Schema(description = "Clave de la foto, para confirmarla después de subirla.",
                example = "leagues/" + ApiExamples.LEAGUE_ID + "/players/" + ApiExamples.PLAYER_ID
                        + "/0b6f2c1a-5d4e-4f3a-9b8c-7d6e5f4a3b2c.webp")
        String photoKey,
        @Schema(description = "Hasta cuándo vale la URL (`squad.photos.s3.upload-ttl`).",
                example = "2026-01-11T10:05:00Z")
        Instant expiresAt) {
}
