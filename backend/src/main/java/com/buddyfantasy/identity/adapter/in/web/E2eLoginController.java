package com.buddyfantasy.identity.adapter.in.web;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FE-34: the E2E tests log in without Google, only with the {@code e2e} profile, which production never activates.
 * The user is registered as Google's login does (subject {@code e2e:<email>}) and the session gets the same
 * {@link CurrentUser} principal, so the rest of the app cannot tell the difference. CSRF applies as to any POST.
 */
@Hidden
@Profile(E2eLoginController.PROFILE)
@RestController
@RequiredArgsConstructor
class E2eLoginController {

    static final String PROFILE = "e2e";
    static final String PATH = "/api/e2e/login";

    private static final Logger log = LoggerFactory.getLogger(E2eLoginController.class);

    private final CommandBus bus;
    private final Clock clock;
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @PostConstruct
    void warn() {
        log.warn("Profile '{}' active: {} logs anyone in without Google. Never use it in production.", PROFILE, PATH);
    }

    @PostMapping(PATH)
    ResponseEntity<Void> logIn(@Valid @RequestBody E2eLoginRequest login, HttpServletRequest request,
                               HttpServletResponse response) {
        String subject = "e2e:" + login.email();
        UserId id = bus.dispatch(new RegisterOrLoginUserCommand(subject, login.email(), login.displayName(), null));

        OidcIdToken idToken = new OidcIdToken(PROFILE, clock.instant(), null,
                Map.of("sub", subject, "email", login.email(), "name", login.displayName()));
        CurrentUser user = new CurrentUser(List.of(new OidcUserAuthority(idToken)), idToken, null, id.value());
        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), PROFILE));

        // a new session id on login (session fixation), as Google's login does
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        holder.setContext(context);
        contexts.saveContext(context, request, response);
        return ResponseEntity.noContent().build();
    }
}
