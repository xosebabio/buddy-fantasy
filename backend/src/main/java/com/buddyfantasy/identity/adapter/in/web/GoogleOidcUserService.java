package com.buddyfantasy.identity.adapter.in.web;

import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Single adapter coupled to Google: registers the user on login and swaps the principal for {@link CurrentUser}. */
@Component
@RequiredArgsConstructor
class GoogleOidcUserService extends OidcUserService {

    private final CommandBus bus;

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        return register(super.loadUser(request));
    }

    OidcUser register(OidcUser google) {
        if (!Boolean.TRUE.equals(google.getEmailVerified())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"));
        }
        UserId id = bus.dispatch(new RegisterOrLoginUserCommand(
                google.getSubject(), google.getEmail(), displayName(google), google.getPicture()));
        return new CurrentUser(google.getAuthorities(), google.getIdToken(), google.getUserInfo(), id.value());
    }

    private static String displayName(OidcUser google) {
        String name = google.getFullName();
        return name != null && !name.isBlank() ? name : google.getEmail();
    }
}
