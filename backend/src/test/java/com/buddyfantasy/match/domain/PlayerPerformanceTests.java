package com.buddyfantasy.match.domain;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerPerformanceTests {

    static final UUID PLAYER = UUID.randomUUID();

    static PlayerPerformance counters(int goals, int penaltyGoals, int penaltiesSaved, int penaltiesMissed,
            int assists, int ownGoals) {
        return new PlayerPerformance(PLAYER, Position.FORWARD, goals, penaltyGoals, penaltiesSaved,
                penaltiesMissed, assists, ownGoals, false, false, false);
    }

    static PlayerPerformance cards(boolean yellow, boolean doubleYellow, boolean red) {
        return new PlayerPerformance(PLAYER, Position.DEFENDER, 0, 0, 0, 0, 0, 0, yellow, doubleYellow, red);
    }

    @Test
    void acceptsCountersWithinRange() {
        assertThatCode(() -> counters(0, 0, 0, 0, 0, 0)).doesNotThrowAnyException();
        assertThatCode(() -> counters(99, 99, 99, 99, 99, 99)).doesNotThrowAnyException();
    }

    @Test
    void rejectsCountersOutOfRange() {
        assertThatThrownBy(() -> counters(-1, 0, 0, 0, 0, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> counters(0, 100, 0, 0, 0, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> counters(0, 0, -1, 0, 0, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> counters(0, 0, 0, 100, 0, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> counters(0, 0, 0, 0, -1, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> counters(0, 0, 0, 0, 0, 100)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void requiresPlayerAndPosition() {
        assertThatThrownBy(() -> new PlayerPerformance(null, Position.FORWARD, 0, 0, 0, 0, 0, 0,
                false, false, false)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PlayerPerformance(PLAYER, null, 0, 0, 0, 0, 0, 0,
                false, false, false)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void doubleYellowExcludesYellowAndStraightRed() {
        assertThatThrownBy(() -> cards(true, true, false)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> cards(false, true, true)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> cards(true, true, true)).isInstanceOf(DomainValidationException.class);
        assertThatCode(() -> cards(false, true, false)).doesNotThrowAnyException();
    }

    @Test
    void yellowAndStraightRedAddUp() {
        PlayerPerformance performance = cards(true, false, true);

        assertThat(performance.yellowCard()).isTrue();
        assertThat(performance.redCard()).isTrue();
    }

    @Test
    void toDataCopiesEveryField() {
        PlayerPerformance performance = new PlayerPerformance(PLAYER, Position.GOALKEEPER, 1, 2, 3, 4, 5, 6,
                true, false, true);

        assertThat(performance.toData()).isEqualTo(new PlayerPerformanceData(PLAYER, Position.GOALKEEPER,
                1, 2, 3, 4, 5, 6, true, false, true));
    }
}
