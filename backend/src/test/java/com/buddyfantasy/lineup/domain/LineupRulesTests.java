package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Size, formation, budget and changes of a lineup (RF-24, RF-26, RF-29). */
class LineupRulesTests {

    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant SAVED = Instant.parse("2026-01-10T10:00:00Z");

    /** Lineups of 5, at most 2 changes; formation and budget as given. */
    static LineupSettings settings(boolean formationEnabled, int minGoalkeepers, int minDefenders,
            int minMidfielders, int minForwards, boolean budgetEnabled, String budget) {
        return new LineupSettings(5, 2, 2, formationEnabled, minGoalkeepers, minDefenders, minMidfielders,
                minForwards, budgetEnabled, new BigDecimal(budget));
    }

    static final LineupSettings PLAIN = settings(false, 1, 1, 1, 1, false, "100");

    final Map<UUID, LineupPlayer> players = new HashMap<>();

    UUID player(Position position, String value) {
        return player(position, value, true);
    }

    UUID player(Position position, String value, boolean active) {
        UUID id = UUID.randomUUID();
        players.put(id, new LineupPlayer(id, position, active, new BigDecimal(value)));
        return id;
    }

    /** One per position plus an extra forward, 10 M each (50 M in total). */
    List<UUID> balancedLineup() {
        return new ArrayList<>(List.of(player(Position.GOALKEEPER, "10"), player(Position.DEFENDER, "10"),
                player(Position.MIDFIELDER, "10"), player(Position.FORWARD, "10"), player(Position.FORWARD, "10")));
    }

    static LineupVersion baseline(List<UUID> playerIds) {
        return LineupVersion.save(LEAGUE, USER, playerIds, SAVED);
    }

    static void assertInvalid(Runnable action, LineupError code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo(code);
    }

    @Test
    void aValidLineupPasses() {
        assertThatCode(() -> new LineupRules(PLAIN).requireValid(balancedLineup(), players))
                .doesNotThrowAnyException();
    }

    @Test
    void theLineupNeedsExactlyTheConfiguredSize() {
        LineupRules rules = new LineupRules(PLAIN);
        List<UUID> lineup = balancedLineup();

        assertInvalid(() -> rules.requireValid(lineup.subList(0, 4), players), LineupError.SIZE);
        lineup.add(player(Position.FORWARD, "1"));
        assertInvalid(() -> rules.requireValid(lineup, players), LineupError.SIZE);
        assertInvalid(() -> rules.requireValid(List.of(), players), LineupError.SIZE);
    }

    @Test
    void aPlayerCannotAppearTwice() {
        List<UUID> lineup = balancedLineup();
        lineup.set(4, lineup.get(3));

        assertInvalid(() -> new LineupRules(PLAIN).requireValid(lineup, players), LineupError.DUPLICATE_PLAYER);
    }

    @Test
    void retiredPlayersCannotBeLinedUp() {
        List<UUID> lineup = balancedLineup();
        lineup.set(0, player(Position.GOALKEEPER, "10", false));

        assertInvalid(() -> new LineupRules(PLAIN).requireValid(lineup, players), LineupError.INACTIVE_PLAYER);
    }

    @Test
    void playersOutsideTheLeaguesSquadAreUnknown() {
        List<UUID> lineup = balancedLineup();
        lineup.set(0, UUID.randomUUID());

        assertInvalid(() -> new LineupRules(PLAIN).requireValid(lineup, players), LineupError.UNKNOWN_PLAYER);
    }

    @Test
    void withTheFormationEnabledEachPositionNeedsItsMinimum() {
        LineupRules rules = new LineupRules(settings(true, 1, 2, 1, 1, false, "100"));
        List<UUID> oneDefender = balancedLineup();
        List<UUID> twoDefenders = balancedLineup();
        twoDefenders.set(4, player(Position.DEFENDER, "10"));

        assertInvalid(() -> rules.requireValid(oneDefender, players), LineupError.FORMATION);
        assertThatCode(() -> rules.requireValid(twoDefenders, players)).doesNotThrowAnyException();
    }

    @Test
    void aZeroMinimumAllowsLeavingThePositionOut() {
        LineupRules rules = new LineupRules(settings(true, 1, 1, 0, 1, false, "100"));
        List<UUID> lineup = balancedLineup();
        lineup.set(2, player(Position.DEFENDER, "10"));

        assertThatCode(() -> rules.requireValid(lineup, players)).doesNotThrowAnyException();
    }

    @Test
    void withTheFormationDisabledPositionsDoNotMatter() {
        List<UUID> forwards = List.of(player(Position.FORWARD, "1"), player(Position.FORWARD, "1"),
                player(Position.FORWARD, "1"), player(Position.FORWARD, "1"), player(Position.FORWARD, "1"));

        assertThatCode(() -> new LineupRules(settings(false, 1, 1, 1, 1, false, "100"))
                .requireValid(forwards, players)).doesNotThrowAnyException();
    }

    @Test
    void exactlyTheBudgetFitsAndAboveItIsRejected() {
        List<UUID> lineup = balancedLineup();

        assertThatCode(() -> new LineupRules(settings(false, 0, 0, 0, 0, true, "50"))
                .requireValid(lineup, players)).doesNotThrowAnyException();
        assertInvalid(() -> new LineupRules(settings(false, 0, 0, 0, 0, true, "49.99"))
                .requireValid(lineup, players), LineupError.OVER_BUDGET);
    }

    @Test
    void withTheBudgetDisabledAnyValueFits() {
        LineupRules rules = new LineupRules(settings(false, 0, 0, 0, 0, false, "1"));

        assertThatCode(() -> rules.requireValid(balancedLineup(), players)).doesNotThrowAnyException();
        assertThat(rules.isOverBudget(new BigDecimal("1000"))).isFalse();
    }

    @Test
    void totalValueAddsTheCurrentValues() {
        List<UUID> lineup = List.of(player(Position.FORWARD, "12.5"), player(Position.DEFENDER, "7.25"));

        assertThat(new LineupRules(PLAIN).totalValue(lineup, players)).isEqualByComparingTo("19.75");
    }

    @Test
    void withoutABaselineChangesAreFree() {
        LineupRules rules = new LineupRules(PLAIN);

        assertThat(rules.changes(Optional.empty(), balancedLineup(), players)).isZero();
        assertThatCode(() -> rules.requireWithinMaxChanges(Optional.empty(), balancedLineup(), players))
                .doesNotThrowAnyException();
    }

    @Test
    void changesAreTheNewPlayersAgainstTheBaseline() {
        List<UUID> previous = balancedLineup();
        List<UUID> lineup = new ArrayList<>(previous);
        lineup.set(3, player(Position.FORWARD, "10"));
        lineup.set(4, player(Position.FORWARD, "10"));

        assertThat(new LineupRules(PLAIN).changes(Optional.of(baseline(previous)), lineup, players)).isEqualTo(2);
    }

    @Test
    void puttingAndRemovingAPlayerIsNoChange() {
        List<UUID> previous = balancedLineup();
        List<UUID> reordered = List.of(previous.get(4), previous.get(3), previous.get(2), previous.get(1),
                previous.get(0));

        // the final lineup is the baseline again, whatever was saved in between
        assertThat(new LineupRules(PLAIN).changes(Optional.of(baseline(previous)), reordered, players)).isZero();
    }

    @Test
    void replacingARetiredPlayerIsFree() {
        List<UUID> previous = balancedLineup();
        UUID retired = previous.get(0);
        players.put(retired, new LineupPlayer(retired, Position.GOALKEEPER, false, BigDecimal.TEN));
        List<UUID> lineup = new ArrayList<>(previous);
        lineup.set(0, player(Position.GOALKEEPER, "10"));
        lineup.set(4, player(Position.FORWARD, "10"));

        assertThat(new LineupRules(PLAIN).changes(Optional.of(baseline(previous)), lineup, players)).isEqualTo(1);
    }

    @Test
    void aRetiredPlayerKeptInTheLineupFreesNoChange() {
        List<UUID> previous = balancedLineup();
        UUID retired = previous.get(0);
        players.put(retired, new LineupPlayer(retired, Position.GOALKEEPER, false, BigDecimal.TEN));
        List<UUID> lineup = new ArrayList<>(previous);
        lineup.set(4, player(Position.FORWARD, "10"));

        assertThat(new LineupRules(PLAIN).changes(Optional.of(baseline(previous)), lineup, players)).isEqualTo(1);
    }

    @Test
    void fillingTheGapAfterTheSizeGrewIsFree() {
        List<UUID> lineup = balancedLineup();
        LineupVersion smaller = baseline(lineup.subList(0, 3));

        // 2 new players fill the 2 new places; replacing one of the 3 is a change
        assertThat(new LineupRules(PLAIN).changes(Optional.of(smaller), lineup, players)).isZero();
        lineup.set(0, player(Position.GOALKEEPER, "10"));
        assertThat(new LineupRules(PLAIN).changes(Optional.of(smaller), lineup, players)).isEqualTo(1);
    }

    @Test
    void oneChangeAboveTheMaximumIsRejected() {
        LineupRules rules = new LineupRules(PLAIN);
        List<UUID> previous = balancedLineup();
        List<UUID> twoChanges = new ArrayList<>(previous);
        twoChanges.set(3, player(Position.FORWARD, "10"));
        twoChanges.set(4, player(Position.FORWARD, "10"));
        List<UUID> threeChanges = new ArrayList<>(twoChanges);
        threeChanges.set(2, player(Position.MIDFIELDER, "10"));
        Optional<LineupVersion> base = Optional.of(baseline(previous));

        assertThatCode(() -> rules.requireWithinMaxChanges(base, twoChanges, players)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.requireWithinMaxChanges(base, threeChanges, players))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).code())
                .isEqualTo(LineupError.MAX_CHANGES_EXCEEDED);
    }

    @Test
    void aValidLineupHasNoWarnings() {
        assertThat(new LineupRules(settings(false, 0, 0, 0, 0, true, "50")).warnings(balancedLineup(), players))
                .isEmpty();
    }

    @Test
    void warnsWhenTheSavedLineupNoLongerFits() {
        List<UUID> lineup = balancedLineup();
        UUID retired = lineup.get(0);
        players.put(retired, new LineupPlayer(retired, Position.GOALKEEPER, false, new BigDecimal("30")));
        LineupRules rules = new LineupRules(new LineupSettings(7, 2, 2, false, 0, 0, 0, 0, true,
                new BigDecimal("50")));

        assertThat(rules.warnings(lineup, players)).containsExactlyInAnyOrder(
                LineupWarning.OVER_BUDGET, LineupWarning.RETIRED_PLAYERS, LineupWarning.SIZE_MISMATCH);
    }
}
