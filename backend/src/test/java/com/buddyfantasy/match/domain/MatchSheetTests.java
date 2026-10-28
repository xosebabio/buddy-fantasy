package com.buddyfantasy.match.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchSheetTests {

    static PlayerPerformance scorer(UUID playerId, int goals, int penaltyGoals, int ownGoals) {
        return new PlayerPerformance(playerId, Position.FORWARD, goals, penaltyGoals, 0, 0, 0, ownGoals,
                false, false, false);
    }

    static PlayerPerformance played(UUID playerId) {
        return scorer(playerId, 0, 0, 0);
    }

    static List<PlayerPerformance> players(int count) {
        return IntStream.range(0, count).mapToObj(i -> played(UUID.randomUUID())).toList();
    }

    @Test
    @DisplayName("Rejects a repeated player")
    void rejectsARepeatedPlayer() {
        UUID player = UUID.randomUUID();

        assertThatThrownBy(() -> new MatchSheet(new MatchResult(1, 0),
                List.of(scorer(player, 1, 0, 0), played(player)))).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Rejects missing or null performances")
    void rejectsMissingOrNullPerformances() {
        assertThatThrownBy(() -> new MatchSheet(null, null)).isInstanceOf(DomainValidationException.class);
        List<PlayerPerformance> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> new MatchSheet(null, withNull)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Rejects goals out of range")
    void rejectsGoalsOutOfRange() {
        assertThatThrownBy(() -> new MatchResult(-1, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new MatchResult(0, 32768)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Is complete only with a result and at least one player")
    void isCompleteOnlyWithResultAndAtLeastOnePlayer() {
        assertThat(MatchSheet.EMPTY.isComplete()).isFalse();
        assertThat(new MatchSheet(new MatchResult(0, 0), List.of()).isComplete()).isFalse();
        assertThat(new MatchSheet(null, players(1)).isComplete()).isFalse();
        assertThat(new MatchSheet(new MatchResult(0, 0), players(1)).isComplete()).isTrue();
    }

    @Test
    @DisplayName("The same lines in another order are the same sheet")
    void sameLinesInAnotherOrderAreTheSameSheet() {
        PlayerPerformance first = scorer(UUID.randomUUID(), 1, 0, 0);
        PlayerPerformance second = played(UUID.randomUUID());

        assertThat(new MatchSheet(new MatchResult(1, 0), List.of(first, second)))
                .isEqualTo(new MatchSheet(new MatchResult(1, 0), List.of(second, first)));
    }

    @Test
    @DisplayName("No warnings when the goals add up")
    void noWarningsWhenGoalsAddUp() {
        MatchSheet sheet = new MatchSheet(new MatchResult(3, 2), List.of(
                scorer(UUID.randomUUID(), 2, 0, 1), scorer(UUID.randomUUID(), 0, 1, 1)));

        assertThat(sheet.warnings()).isEmpty();
    }

    @Test
    @DisplayName("The goals mismatch counts goals and penalty goals")
    void goalsMismatchCountsGoalsAndPenaltyGoals() {
        MatchSheet missingScorer = new MatchSheet(new MatchResult(3, 0), List.of(scorer(UUID.randomUUID(), 1, 1, 0)));
        MatchSheet tooManyGoals = new MatchSheet(new MatchResult(1, 0), List.of(scorer(UUID.randomUUID(), 1, 1, 0)));

        assertThat(missingScorer.warnings()).containsExactly(MatchSheetWarning.GOALS_MISMATCH);
        assertThat(tooManyGoals.warnings()).containsExactly(MatchSheetWarning.GOALS_MISMATCH);
    }

    @Test
    @DisplayName("Warns when own goals exceed the goals against")
    void warnsWhenOwnGoalsExceedGoalsAgainst() {
        MatchSheet sheet = new MatchSheet(new MatchResult(0, 1), List.of(scorer(UUID.randomUUID(), 0, 0, 2)));

        assertThat(sheet.warnings()).containsExactly(MatchSheetWarning.OWN_GOALS_EXCEED_GOALS_AGAINST);
    }

    @Test
    @DisplayName("Reports both warnings at once")
    void reportsBothWarningsAtOnce() {
        MatchSheet sheet = new MatchSheet(new MatchResult(2, 0), List.of(scorer(UUID.randomUUID(), 1, 0, 1)));

        assertThat(sheet.warnings()).containsExactly(MatchSheetWarning.GOALS_MISMATCH,
                MatchSheetWarning.OWN_GOALS_EXCEED_GOALS_AGAINST);
    }

    @Test
    @DisplayName("No warnings without a result")
    void noWarningsWithoutResult() {
        assertThat(new MatchSheet(null, List.of(scorer(UUID.randomUUID(), 3, 0, 2))).warnings()).isEmpty();
    }
}
