package com.buddyfantasy.match.domain;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.match.adapter.out.config.TestMatchLimits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchLimitsTests {

    private final MatchLimits limits = TestMatchLimits.get();

    static PlayerPerformance counters(int goals, int penaltyGoals, int penaltiesSaved, int penaltiesMissed,
                                      int assists, int ownGoals) {
        return new PlayerPerformance(UUID.randomUUID(), Position.FORWARD, goals, penaltyGoals, penaltiesSaved,
                penaltiesMissed, assists, ownGoals, false, false, false);
    }

    static MatchSheet sheet(MatchResult result, PlayerPerformance... performances) {
        return new MatchSheet(result, List.of(performances));
    }

    static MatchSheet players(int count) {
        return new MatchSheet(null, IntStream.range(0, count).mapToObj(i -> counters(0, 0, 0, 0, 0, 0)).toList());
    }

    @Test
    @DisplayName("The configured limits match the requirements")
    void configuredLimitsMatchRequirements() {
        assertThat(limits.opponentMaxLength()).isEqualTo(100);
        assertThat(limits.matchdayMin()).isOne();
        assertThat(limits.goalsMax()).isEqualTo(99);
        assertThat(limits.statCountMax()).isEqualTo(99);
        assertThat(limits.sheetPlayersMax()).isEqualTo(30);
    }

    @Test
    @DisplayName("The opponent must be non-blank and within the maximum length")
    void opponentMustBeNonBlankAndWithinTheMaximumLength() {
        assertThatCode(() -> limits.requireValidOpponent("x".repeat(100))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValidOpponent("x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValidOpponent(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("The matchday must reach the minimum")
    void matchdayMustReachTheMinimum() {
        assertThatCode(() -> limits.requireValid(new Matchday(1))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValid(new Matchday(0))).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new MatchLimits(100, 0, 99, 99, 30).requireValid(new Matchday(0)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The sheet accepts up to the maximum number of players")
    void sheetAcceptsUpToTheMaximumPlayers() {
        assertThatCode(() -> limits.requireValid(players(30))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValid(players(31))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireSheetSize(31)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("The goals of the result must not exceed the maximum")
    void goalsOfTheResultMustNotExceedTheMaximum() {
        assertThatCode(() -> limits.requireValid(sheet(new MatchResult(99, 99)))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValid(sheet(new MatchResult(100, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValid(sheet(new MatchResult(0, 100))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Every stat must not exceed the maximum")
    void everyStatMustNotExceedTheMaximum() {
        assertThatCode(() -> limits.requireValid(sheet(null, counters(99, 99, 99, 99, 99, 99))))
                .doesNotThrowAnyException();
        for (int stat = 0; stat < 6; stat++) {
            int[] values = new int[6];
            values[stat] = 100;
            MatchSheet over = sheet(null, counters(values[0], values[1], values[2], values[3], values[4], values[5]));
            assertThatThrownBy(() -> limits.requireValid(over)).as("stat %d", stat)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Rejects nonsense limits")
    void rejectsNonsenseLimits() {
        assertThatThrownBy(() -> new MatchLimits(0, 1, 99, 99, 30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchLimits(100, -1, 99, 99, 30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchLimits(100, 1, 32768, 99, 30))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchLimits(100, 1, 99, 32768, 30))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchLimits(100, 1, 99, 99, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
