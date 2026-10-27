package com.buddyfantasy.squad.domain;

import com.buddyfantasy.squad.adapter.out.config.TestSquadLimits;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SquadLimitsTests {

    private final SquadLimits limits = TestSquadLimits.get();

    @Test
    void configuredLimitsMatchRequirements() {
        assertThat(limits.nameMaxLength()).isEqualTo(100);
        assertThat(limits.shirtNumberMin()).isZero();
        assertThat(limits.shirtNumberMax()).isEqualTo(99);
        assertThat(limits.photoMaxSizeBytes()).isEqualTo(512 * 1024);
    }

    @Test
    void nameMustBeNonBlankAndWithinTheMaximumLength() {
        assertThatCode(() -> limits.requireValidName("x".repeat(100))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValidName("x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValidName(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shirtNumberMustBeWithinTheConfiguredRangeOrAbsent() {
        assertThatCode(() -> limits.requireValid(null)).doesNotThrowAnyException();
        assertThatCode(() -> limits.requireValid(new ShirtNumber(99))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValid(new ShirtNumber(100)))
                .isInstanceOf(IllegalArgumentException.class);
        SquadLimits from1To50 = new SquadLimits(100, 1, 50, 1024);
        assertThatThrownBy(() -> from1To50.requireValid(new ShirtNumber(0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> from1To50.requireValid(new ShirtNumber(51)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void photoMustBeWebpAndWithinTheMaximumSize() {
        assertThatCode(() -> PlayerPhotoFormat.requireAcceptable("image/webp", 512 * 1024, limits))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PlayerPhotoFormat.requireAcceptable("image/webp", 512 * 1024 + 1, limits))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlayerPhotoFormat.requireAcceptable("image/webp", 0, limits))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlayerPhotoFormat.requireAcceptable("image/png", 1024, limits))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonsenseLimits() {
        assertThatThrownBy(() -> new SquadLimits(0, 0, 99, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SquadLimits(100, -1, 99, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SquadLimits(100, 10, 9, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SquadLimits(100, 0, 32768, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SquadLimits(100, 0, 99, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
