package com.buddyfantasy.squad.domain;

import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerPhotoKeyTests {

    static final UUID LEAGUE = UUID.randomUUID();
    static final PlayerId PLAYER = PlayerId.generate();
    static final String PREFIX = "leagues/" + LEAGUE + "/players/" + PLAYER.value() + "/";

    @Test
    void generatesAWebpKeyUnderThePlayersPrefix() {
        PlayerPhotoKey key = PlayerPhotoKey.generate(LEAGUE, PLAYER);

        assertThat(key.value()).startsWith(PREFIX).matches(".*/[0-9a-f-]{36}\\.webp")
                .hasSizeLessThanOrEqualTo(PlayerPhotoKey.MAX_LENGTH);
        assertThat(key.belongsTo(LEAGUE, PLAYER)).isTrue();
    }

    @Test
    void everyUploadGetsANewKey() {
        assertThat(PlayerPhotoKey.generate(LEAGUE, PLAYER)).isNotEqualTo(PlayerPhotoKey.generate(LEAGUE, PLAYER));
    }

    @Test
    void doesNotBelongToAnotherPlayerOrLeague() {
        PlayerPhotoKey key = PlayerPhotoKey.generate(LEAGUE, PLAYER);

        assertThat(key.belongsTo(LEAGUE, PlayerId.generate())).isFalse();
        assertThat(key.belongsTo(UUID.randomUUID(), PLAYER)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "photo.webp",
            "4b3f1c2e-0000-4000-8000-000000000000.jpg",
            "4b3f1c2e-0000-4000-8000-000000000000.webp/extra",
            "../4b3f1c2e-0000-4000-8000-000000000000.webp",
            "sub/4b3f1c2e-0000-4000-8000-000000000000.webp",
            "4B3F1C2E-0000-4000-8000-000000000000.webp",
            ""})
    void rejectsAnythingButASingleUuidWebpUnderThePrefix(String fileName) {
        assertThat(new PlayerPhotoKey(PREFIX + fileName).belongsTo(LEAGUE, PLAYER)).isFalse();
    }

    @Test
    void rejectsBlankOrTooLongValues() {
        assertThatThrownBy(() -> new PlayerPhotoKey(" ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PlayerPhotoKey(null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PlayerPhotoKey("x".repeat(PlayerPhotoKey.MAX_LENGTH + 1)))
                .isInstanceOf(DomainValidationException.class);
        assertThat(PlayerPhotoKey.ofNullable(null)).isNull();
    }
}
