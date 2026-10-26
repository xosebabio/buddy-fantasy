package com.buddyfantasy.identity.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresTestConfig.class)
class GoogleOidcUserServiceTests {

    @Autowired
    GoogleOidcUserService service;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("delete from identity_user");
    }

    private static OidcUser google(boolean emailVerified) {
        OidcIdToken token = new OidcIdToken("t", Instant.now(), Instant.now().plusSeconds(60), Map.of(
                "sub", "google-sub-1", "email", "a@x.com", "email_verified", emailVerified,
                "name", "Ana", "picture", "http://pic"));
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
    }

    @Test
    void verifiedLoginRegistersTheUserAndExposesItsInternalId() {
        OidcUser principal = service.register(google(true));

        assertThat(principal).isInstanceOf(CurrentUser.class);
        assertThat(((CurrentUser) principal).userId()).isEqualTo(
                jdbc.queryForObject("select id from identity_user where google_sub = 'google-sub-1'",
                        java.util.UUID.class));
    }

    @Test
    void unverifiedEmailIsRejectedAndNothingIsStored() {
        assertThatThrownBy(() -> service.register(google(false))).isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(jdbc.queryForObject("select count(*) from identity_user", Integer.class)).isZero();
    }
}
