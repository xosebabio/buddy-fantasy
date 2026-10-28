package com.buddyfantasy.identity.adapter.in.web;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Google OIDC login with a JDBC-backed session cookie; the API answers 401/204 instead of redirecting, and a login
 * returns to the SPA's {@code /} (or {@code /login?error}). With the {@code e2e} profile, the E2E tests' login is
 * public too ({@link E2eLoginController}).
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleOidcUserService oidcUserService,
                                            Environment environment) throws Exception {
        boolean e2e = environment.acceptsProfiles(Profiles.of(E2eLoginController.PROFILE));
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                            .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                    if (e2e) {
                        auth.requestMatchers(HttpMethod.POST, E2eLoginController.PATH).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/**")))
                // /login is the SPA's page (Spring no longer generates one), public so the backend alone answers 404
                // instead of redirecting to itself; the SPA restores the saved route itself
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                        .userInfoEndpoint(info -> info.oidcUserService(oidcUserService)))
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", "POST"))
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("SESSION"))
                .build();
    }
}
