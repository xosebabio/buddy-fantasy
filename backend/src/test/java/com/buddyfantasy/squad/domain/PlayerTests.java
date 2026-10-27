package com.buddyfantasy.squad.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.squad.PlayerRegistered;
import com.buddyfantasy.squad.PlayerRetired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final UUID LEAGUE = UUID.randomUUID();
    static final BigDecimal VALUE = new BigDecimal("12.50");

    static Player register() {
        return Player.register(LEAGUE, "Iago", new ShirtNumber(9), Position.FORWARD, VALUE, NOW);
    }

    @Test
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
    void shirtNumberAndPhotoAreOptional() {
        Player player = Player.register(LEAGUE, "Iago", null, Position.GOALKEEPER, VALUE, NOW);

        assertThat(player.shirtNumber()).isNull();
        assertThat(player.photoKey()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Player.register(LEAGUE, name, null, Position.FORWARD, VALUE, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsMissingOrTooLongName() {
        assertThatThrownBy(() -> Player.register(LEAGUE, null, null, Position.FORWARD, VALUE, NOW))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Player.register(LEAGUE, "x".repeat(101), null, Position.FORWARD, VALUE, NOW))
                .isInstanceOf(DomainValidationException.class);
        assertThat(Player.register(LEAGUE, "x".repeat(100), null, Position.FORWARD, VALUE, NOW).name())
                .hasSize(100);
    }

    @Test
    void requiresPosition() {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", null, null, VALUE, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 99})
    void acceptsShirtNumbersWithinRange(int number) {
        assertThat(new ShirtNumber(number).value()).isEqualTo(number);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 100})
    void rejectsShirtNumbersOutOfRange(int number) {
        assertThatThrownBy(() -> new ShirtNumber(number)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shirtNumberOfNullableKeepsNull() {
        assertThat(ShirtNumber.ofNullable(null)).isNull();
        assertThat(ShirtNumber.ofNullable(7)).isEqualTo(new ShirtNumber(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "0.001", "100000000"})
    void rejectsInitialValuesThatAreNotPositiveOrDoNotFitTheColumn(String value) {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", null, Position.FORWARD, new BigDecimal(value),
                NOW)).isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "14", "14.50", "99999999.99"})
    void acceptsPositiveInitialValuesInMillions(String value) {
        assertThat(Player.register(LEAGUE, "Iago", null, Position.FORWARD, new BigDecimal(value), NOW)
                .initialValue()).isEqualByComparingTo(value);
    }

    @Test
    void rejectsMissingInitialValue() {
        assertThatThrownBy(() -> Player.register(LEAGUE, "Iago", null, Position.FORWARD, null, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void updateReplacesEveryEditableFieldButKeepsThePhoto() {
        Player player = register();
        PlayerPhotoKey photoKey = PlayerPhotoKey.generate(LEAGUE, player.id());
        player.changePhoto(photoKey);

        player.update("Iago Aspas", null, Position.MIDFIELDER, new BigDecimal("20"));

        assertThat(player.name()).isEqualTo("Iago Aspas");
        assertThat(player.shirtNumber()).isNull();
        assertThat(player.position()).isEqualTo(Position.MIDFIELDER);
        assertThat(player.initialValue()).isEqualByComparingTo("20");
        assertThat(player.photoKey()).isEqualTo(photoKey);
    }

    @Test
    void updateValidatesLikeRegister() {
        Player player = register();

        assertThatThrownBy(() -> player.update("", null, Position.FORWARD, VALUE))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> player.update("Iago", null, Position.FORWARD, BigDecimal.ZERO))
                .isInstanceOf(DomainValidationException.class);
        assertThat(player.name()).isEqualTo("Iago");
    }

    @Test
    void retiredPlayerCannotBeEdited() {
        Player player = register();
        player.retire(NOW);

        assertThatThrownBy(() -> player.update("Iago", null, Position.FORWARD, VALUE))
                .isInstanceOf(ConflictException.class);
    }

    @Test
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
    void retiredPlayerCannotChangePhoto() {
        Player player = register();
        player.retire(NOW);

        assertThatThrownBy(() -> player.changePhoto(PlayerPhotoKey.generate(LEAGUE, player.id())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void removePhotoIsIdempotentAndAllowedOnRetiredPlayers() {
        Player player = register();
        player.changePhoto(PlayerPhotoKey.generate(LEAGUE, player.id()));
        player.retire(NOW);

        player.removePhoto();
        player.removePhoto();

        assertThat(player.photoKey()).isNull();
    }

    @Test
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
