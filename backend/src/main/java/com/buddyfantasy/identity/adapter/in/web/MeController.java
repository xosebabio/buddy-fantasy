package com.buddyfantasy.identity.adapter.in.web;

import com.buddyfantasy.identity.application.usecases.getcurrentuser.GetCurrentUserQuery;
import com.buddyfantasy.identity.application.usecases.getcurrentuser.UserView;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class MeController {

    private final CommandBus bus;

    @GetMapping("/api/me")
    UserView me(@AuthenticationPrincipal CurrentUser user) {
        return bus.ask(new GetCurrentUserQuery(user.userId()));
    }
}
