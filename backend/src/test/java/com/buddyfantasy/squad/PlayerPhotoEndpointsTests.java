package com.buddyfantasy.squad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.squad.application.usecases.confirmplayerphoto.ConfirmPlayerPhotoCommand;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.PlayerPhotoKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfig.class, InMemoryImageStorage.Config.class})
class PlayerPhotoEndpointsTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final String WEBP = "image/webp";
    static final long MAX_BYTES = 512 * 1024;

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InMemoryImageStorage storage;

    UUID admin;
    UUID ana;
    UUID outsider;
    UUID league;
    UUID otherLeague;
    UUID player;

    @BeforeEach
    void setUp() {
        storage.reset();
        jdbc.update("delete from squad_player");
        jdbc.update("delete from league_membership");
        jdbc.update("delete from league_invitation");
        jdbc.update("delete from league_scoring_rule");
        jdbc.update("delete from league_settings");
        jdbc.update("delete from league_league");
        jdbc.update("delete from identity_user");
        admin = register("admin");
        ana = register("ana");
        outsider = register("outsider");
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(admin, "Peña"));
        league = created.id().value();
        bus.dispatch(new JoinLeagueCommand(ana, created.invitationToken()));
        // outsider is ADMIN of another league, never a member of "Peña"
        otherLeague = bus.dispatch(new CreateLeagueCommand(outsider, "Otra")).id().value();
        player = addPlayer(league, admin, "Iago", 10);
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, null)).value();
    }

    private static RequestPostProcessor as(UUID userId) {
        OidcIdToken idToken = new OidcIdToken("t", NOW, NOW.plusSeconds(3600), Map.of("sub", "any"));
        return oidcLogin().oidcUser(new CurrentUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken, null, userId));
    }

    private UUID addPlayer(UUID leagueId, UUID leagueAdmin, String name, Integer shirtNumber) {
        return bus.dispatch(new RegisterPlayerCommand(leagueAdmin, leagueId, name, shirtNumber, Position.FORWARD,
                new BigDecimal("10"))).value();
    }

    private String photoKeyOf(UUID playerId) {
        return jdbc.queryForObject("select photo_key from squad_player where id = ?", String.class, playerId);
    }

    private static String uploadBody(String contentType, Long sizeBytes) {
        return """
                {"contentType": %s, "sizeBytes": %s}"""
                .formatted(contentType == null ? "null" : "\"" + contentType + "\"", sizeBytes);
    }

    private static String confirmBody(String photoKey) {
        return """
                {"photoKey": %s}""".formatted(photoKey == null ? "null" : "\"" + photoKey + "\"");
    }

    /** Requests a URL as the admin and simulates the browser's upload of what was declared. */
    private String uploadedKey(UUID playerId) {
        String key = PlayerPhotoKey.generate(league, new PlayerId(playerId)).value();
        storage.upload(key, WEBP, 30_000);
        return key;
    }

    private void confirm(UUID playerId, String key) {
        bus.dispatch(new ConfirmPlayerPhotoCommand(admin, league, playerId, key));
    }

    @Test
    void adminGetsAPresignedUploadForANewKeyAndNothingIsStoredYet() throws Exception {
        String key = mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player)
                        .with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(uploadBody(WEBP, 30_000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").exists())
                .andExpect(jsonPath("$.headers['content-type']").value(WEBP))
                .andExpect(jsonPath("$.expiresAt").value(InMemoryImageStorage.EXPIRES_AT.toString()))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"photoKey\":\"([^\"]+)\".*", "$1");

        assertThat(key).matches("leagues/" + league + "/players/" + player + "/[0-9a-f-]{36}\\.webp");
        assertThat(photoKeyOf(player)).isNull();
    }

    @Test
    void onlyWebpUpTo512KbIsAccepted() throws Exception {
        for (String body : List.of(uploadBody("image/jpeg", 30_000L), uploadBody("image/png", 30_000L),
                uploadBody(WEBP, 0L), uploadBody(WEBP, -1L), uploadBody(WEBP, MAX_BYTES + 1),
                uploadBody(null, 30_000L), uploadBody(WEBP, null))) {
            mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player)
                            .with(as(admin)).with(csrf()).contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player)
                        .with(as(admin)).with(csrf()).contentType(APPLICATION_JSON).content(uploadBody(WEBP, MAX_BYTES)))
                .andExpect(status().isOk());
    }

    @Test
    void uploadRequiresTheLeagueAdminAnActivePlayerAndTheRightLeague() throws Exception {
        String body = uploadBody(WEBP, 30_000L);
        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player).with(as(ana)).with(csrf())
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player).with(as(outsider)).with(csrf())
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isForbidden());

        UUID foreign = addPlayer(otherLeague, outsider, "De otra liga", 5);
        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, foreign).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isNotFound());

        bus.dispatch(new RetirePlayerCommand(admin, league, player));
        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isConflict());
    }

    @Test
    void confirmingAnUploadedPhotoSetsItAndExposesItsUrl() throws Exception {
        String key = uploadedKey(player);

        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(confirmBody(key)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.test/" + key));

        assertThat(photoKeyOf(player)).isEqualTo(key);
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.test/" + key));
        mvc.perform(get("/api/leagues/{id}/players", league).with(as(ana)))
                .andExpect(jsonPath("$[0].photoUrl").value("https://cdn.test/" + key));
        assertThat(storage.deleted()).isEmpty();
    }

    @Test
    void playersWithoutPhotoHaveNoUrl() throws Exception {
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    void confirmRejectsKeysOfAnotherPlayerOrLeagueMissingObjectsAndInvalidFiles() throws Exception {
        UUID teammate = addPlayer(league, admin, "Hugo", 9);
        String teammatesKey = uploadedKey(teammate);
        String otherLeaguesKey = PlayerPhotoKey.generate(otherLeague, new PlayerId(player)).value();
        storage.upload(otherLeaguesKey, WEBP, 30_000);
        String notUploaded = PlayerPhotoKey.generate(league, new PlayerId(player)).value();
        String png = PlayerPhotoKey.generate(league, new PlayerId(player)).value();
        storage.upload(png, "image/png", 30_000);
        String tooBig = PlayerPhotoKey.generate(league, new PlayerId(player)).value();
        storage.upload(tooBig, WEBP, MAX_BYTES + 1);

        for (String body : List.of(confirmBody(teammatesKey), confirmBody(otherLeaguesKey), confirmBody(notUploaded),
                confirmBody(png), confirmBody(tooBig), confirmBody("x"), confirmBody(" "), confirmBody(null))) {
            mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf())
                    .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertThat(photoKeyOf(player)).isNull();
    }

    @Test
    void confirmingANewPhotoDeletesThePreviousOneAndRepeatingDeletesNothing() throws Exception {
        String first = uploadedKey(player);
        confirm(player, first);
        String second = uploadedKey(player);

        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(second))).andExpect(status().isOk());
        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(second))).andExpect(status().isOk());

        assertThat(photoKeyOf(player)).isEqualTo(second);
        assertThat(storage.deleted()).containsExactly(first);
    }

    @Test
    void aStorageFailureWhenDeletingThePreviousPhotoDoesNotFailTheChange() throws Exception {
        confirm(player, uploadedKey(player));
        String second = uploadedKey(player);
        storage.failDeletes();

        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(second))).andExpect(status().isOk());
        assertThat(photoKeyOf(player)).isEqualTo(second);
    }

    @Test
    void confirmRequiresTheLeagueAdminAndAnActivePlayer() throws Exception {
        String key = uploadedKey(player);
        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(ana)).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(key))).andExpect(status().isForbidden());
        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(outsider)).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(key))).andExpect(status().isForbidden());

        bus.dispatch(new RetirePlayerCommand(admin, league, player));
        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(key))).andExpect(status().isConflict());
        assertThat(photoKeyOf(player)).isNull();
    }

    @Test
    void removingThePhotoDeletesItAndIsIdempotent() throws Exception {
        String key = uploadedKey(player);
        confirm(player, key);

        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(photoKeyOf(player)).isNull();
        assertThat(storage.deleted()).containsExactly(key);
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    void theAdminCanStillRemoveThePhotoOfARetiredPlayer() throws Exception {
        String key = uploadedKey(player);
        confirm(player, key);
        bus.dispatch(new RetirePlayerCommand(admin, league, player));

        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(photoKeyOf(player)).isNull();
        assertThat(storage.deleted()).containsExactly(key);
    }

    @Test
    void removeRequiresTheLeagueAdminAndTheRightLeague() throws Exception {
        String key = uploadedKey(player);
        confirm(player, key);
        UUID foreign = addPlayer(otherLeague, outsider, "De otra liga", 5);

        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(as(ana)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(as(outsider)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, foreign).with(as(admin)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(photoKeyOf(player)).isEqualTo(key);
        assertThat(storage.deleted()).isEmpty();
    }

    @Test
    void editingThePlayerKeepsThePhoto() throws Exception {
        String key = uploadedKey(player);
        confirm(player, key);

        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Iago Aspas", "shirtNumber": 10, "position": "FORWARD", "initialValue": 12}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.test/" + key));
    }

    @Test
    void securityWithoutSessionOrCsrf() throws Exception {
        String key = uploadedKey(player);

        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player).with(csrf())
                .contentType(APPLICATION_JSON).content(uploadBody(WEBP, 30_000L))).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(csrf())
                .contentType(APPLICATION_JSON).content(confirmBody(key))).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(csrf()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/leagues/{id}/players/{p}/photo/upload", league, player).with(as(admin))
                .contentType(APPLICATION_JSON).content(uploadBody(WEBP, 30_000L))).andExpect(status().isForbidden());
        mvc.perform(put("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin))
                .contentType(APPLICATION_JSON).content(confirmBody(key))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/players/{p}/photo", league, player).with(as(admin)))
                .andExpect(status().isForbidden());
        assertThat(photoKeyOf(player)).isNull();
    }
}
