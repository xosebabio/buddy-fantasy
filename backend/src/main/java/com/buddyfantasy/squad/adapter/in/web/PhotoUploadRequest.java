package com.buddyfantasy.squad.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** What the browser is about to upload; the domain checks the type and size. */
@Schema(description = "La foto que el navegador va a subir.")
record PhotoUploadRequest(
        @Schema(description = "Tipo de la imagen: solo `image/webp`.", example = "image/webp")
        @NotBlank String contentType,
        @Schema(description = "Tamaño en bytes, de 1 a `squad.limits.photo-max-size`.", example = "30000")
        @NotNull Long sizeBytes) {
}
