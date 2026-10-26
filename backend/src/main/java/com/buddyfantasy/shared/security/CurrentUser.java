package com.buddyfantasy.shared.security;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/** Session principal carrying the internal user id, so no module needs identity's classes. */
public class CurrentUser extends DefaultOidcUser {

    private final UUID userId;

    public CurrentUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
                       OidcUserInfo userInfo, UUID userId) {
        super(authorities, idToken, userInfo);
        this.userId = userId;
    }

    public UUID userId() {
        return userId;
    }

    public static Optional<CurrentUser> from(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
