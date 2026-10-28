package com.buddyfantasy.identity.adapter.in.web;

import com.buddyfantasy.identity.application.usecases.getcurrentuser.GetCurrentUserQuery;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cuenta", description = "El usuario con la sesión iniciada (RF-01 a RF-04).")
@RestController
@RequiredArgsConstructor
class MeController {

    private final CommandBus bus;

    @Operation(summary = "Ver mi usuario",
            description = "Devuelve el usuario de la sesión. La interfaz lo usa para saber si hay sesión: sin ella "
                    + "responde `401` y hay que iniciar el login con Google.")
    @ApiResponse(responseCode = "200", description = "El usuario de la sesión.")
    @GetMapping("/api/me")
    UserResponse me(@AuthenticationPrincipal CurrentUser user) {
        return UserResponse.from(bus.ask(new GetCurrentUserQuery(user.userId())));
    }
}
