package com.buddyfantasy.squad.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.squad.PlayerRegistered;
import com.buddyfantasy.squad.PlayerRetired;
import com.buddyfantasy.squad.adapter.out.config.TestSquadLimits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final UUID LEAGUE = UUID.randomUUID();
    static final BigDecimal VALUE = new BigDecimal("12.50");
    static final SquadLimits LIMITS = TestSquadLimits.get();

    static Player register() {
        return Player.register(LEAGUE, "Iago", new ShirtNumber(9), Position.FORWARD, VALUE, NOW, LIMITS);
    }

    @Test
    @DisplayName("register creates an active player and publishes PlayerRegistered once")
    void registerCreatesAnActivePlayerAndPublishesPlayerRegisteredOnce() {
        Player player = register();

        assertThat(player.isRetired()).isFalse();
        assertThat(player.leagueId()).isEqualTo(LEAGUE);
        assertThat(player.createdAt()).isEqualTo(NOW);
        assertThat(player.pullEvents()).containsExactly(
                new PlayerRegistered(LEAGUE, player.id().value(), VALUE, NOW));
        assertThat(player.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("The shirt number and photo are optional")
    void shirtNumberAndPhotoAreOptional() {
        Player player = Player.register(LEAGUE, "Iago", null, Position.GOALKEEPER, VALUE, NOW, LIMITS);

        assertThat(player.shirtNumber()).isNull();
        assertThat(player.photoKey()).isNull();
    }

    @ParameterizedTest
    @DisplayName("Rejects a blank name")
    @ValueSource(strings = {"", "   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Player.register(LEAGUE, name, null, Position.FORWARD, VALUE, NOW, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Rejects a missing or too long name")
    void rejectsMissingOrTooLongName() {
        assertThatThrownBy(() -> Player.register(LEAGUE, null, null, Position.FORWARD, VALUE, NOW, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Player.register(LEAGUE, "x".repeat(101), null, Position.FORWARD, VALUE, NOW, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThat(Player.register(LEAGUE, "x".repeat(100), null, Position.FORWARD, VALUE, NOW, LIMITS).name())
                .hasSize(100);
    }

    @Test
    @DisplayName("Requires a position")
    void requiresPosition() {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", null, null, VALUE, NOW, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @DisplayName("Accepts shirt numbers within the configured range")
    @ValueSource(ints = {0, 99})
    void acceptsShirtNumbersWithinTheConfiguredRange(int number) {
        assertThat(Player.register(LEAGUE, "Iago", new ShirtNumber(number), Position.FORWARD, VALUE, NOW, LIMITS)
                .shirtNumber().value()).isEqualTo(number);
    }

    @Test
    @DisplayName("Rejects shirt numbers out of the configured range")
    void rejectsShirtNumbersOutOfTheConfiguredRange() {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", new ShirtNumber(100), Position.FORWARD, VALUE,
                NOW, LIMITS)).isInstanceOf(DomainValidationException.class);
        Player player = register();
        assertThatThrownBy(() -> player.update("Iago", new ShirtNumber(100), Position.FORWARD, VALUE, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @DisplayName("Rejects shirt numbers out of the storage range")
    @ValueSource(ints = {-1, 32768})
    void rejectsShirtNumbersOutOfStorageRange(int number) {
        assertThatThrownBy(() -> new ShirtNumber(number)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("ShirtNumber.ofNullable keeps null")
    void shirtNumberOfNullableKeepsNull() {
        assertThat(ShirtNumber.ofNullable(null)).isNull();
        assertThat(ShirtNumber.ofNullable(7)).isEqualTo(new ShirtNumber(7));
    }

    @ParameterizedTest
    @DisplayName("Rejects initial values that are not positive or do not fit the column")
    @ValueSource(strings = {"0", "0.00", "-1", "0.001", "100000000"})
    void rejectsInitialValuesThatAreNotPositiveOrDoNotFitTheColumn(String value) {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", null, Position.FORWARD, new BigDecimal(value),
                NOW, LIMITS)).isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @DisplayName("Accepts positive initial values in millions")
    @ValueSource(strings = {"0.01", "14", "14.50", "99999999.99"})
    void acceptsPositiveInitialValuesInMillions(String value) {
        assertThat(Player.register(LEAGUE, "Iago", null, Position.FORWARD, new BigDecimal(value), NOW, LIMITS)
                .initialValue()).isEqualByComparingTo(value);
    }

    @Test
    @DisplayName("Rejects a missing initial value")
    void rejectsMissingInitialValue() {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", null, Position.FORWARD, null, NOW, LIMITS))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("update replaces every editable field but keeps the photo")
    void updateReplacesEveryEditableFieldButKeepsThePhoto() {
        Player player = register();
        PlayerPhotoKey photoKey = PlayerPhotoKey.generate(LEAGUE, player.id());
        player.changePhoto(photoKey);

        player.update("Iago Aspas", null, Position.MIDFIELDER, new BigDecimal("20"), LIMITS);

        assertThat(player.name()).isEqualTo("Iago Aspas");
        assertThat(player.shirtNumber()).isNull();
        assertThat(player.position()).isEqualTo(Position.MIDFIELDER);
        assertThat(player.initialValue()).isEqualByComparingTo("20");
        assertThat(player.photoKey()).isEqualTo(photoKey);
    }

    @Test
    @DisplayName("update validates like register")
    void updateValidatesLikeRegister() {
        Player player = register();

        assertThatThrownBy(() -> player.update("", null, Position.FORWARD, VALUE, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> player.update("Iago", null, Position.FORWARD, BigDecimal.ZERO, LIMITS))
                .isInstanceOf(DomainValidationException.class);
        assertThat(player.name()).isEqualTo("Iago");
    }

    @Test
    @DisplayName("A retired player cannot be edited")
    void retiredPlayerCannotBeEdited() {
        Player player = register();
        player.retire(NOW);

        assertThatThrownBy(() -> player.update("Iago", null, Position.FORWARD, VALUE, LIMITS))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("changePhoto sets a key generated for this player and is idempotent")
    void changePhotoSetsAKeyGeneratedForThisPlayerAndIsIdempotent() {
        Player player = register();
        PlayerPhotoKey photo = PlayerPhotoKey.generate(LEAGUE, player.id());

        player.changePhoto(photo);
        player.changePhoto(photo);
        assertThat(player.photoKey()).isEqualTo(photo);

        PlayerPhotoKey newer = PlayerPhotoKey.generate(LEAGUE, player.id());
        player.changePhoto(newer);
        assertThat(player.photoKey()).isEqualTo(newer);
    }

    @Test
    @DisplayName("changePhoto rejects keys of another player or league")
    void changePhotoRejectsKeysOfAnotherPlayerOrLeague() {
        Player player = register();

        assertThatThrownBy(() -> player.changePhoto(PlayerPhotoKey.generate(LEAGUE, PlayerId.generate())))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> player.changePhoto(PlayerPhotoKey.generate(UUID.randomUUID(), player.id())))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> player.changePhoto(null)).isInstanceOf(DomainValidationException.class);
        assertThat(player.photoKey()).isNull();
    }

    @Test
    @DisplayName("A retired player cannot change photo")
    void retiredPlayerCannotChangePhoto() {
        Player player = register();
        player.retire(NOW);

        assertThatThrownBy(() -> player.changePhoto(PlayerPhotoKey.generate(LEAGUE, player.id())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("removePhoto is idempotent and allowed on retired players")
    void removePhotoIsIdempotentAndAllowedOnRetiredPlayers() {
        Player player = register();
        player.changePhoto(PlayerPhotoKey.generate(LEAGUE, player.id()));
        player.retire(NOW);

        player.removePhoto();
        player.removePhoto();

        assertThat(player.photoKey()).isNull();
    }

    @Test
    @DisplayName("retire is idempotent, keeps the first instant and publishes once")
    void retireIsIdempotentKeepsTheFirstInstantAndPublishesOnce() {
        Player player = register();
        player.pullEvents();

        player.retire(NOW.plusSeconds(10));
        player.retire(NOW.plusSeconds(99));

        assertThat(player.isRetired()).isTrue();
        assertThat(player.retiredAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(player.pullEvents()).containsExactly(
                new PlayerRetired(LEAGUE, player.id().value(), NOW.plusSeconds(10)));
    }
}
