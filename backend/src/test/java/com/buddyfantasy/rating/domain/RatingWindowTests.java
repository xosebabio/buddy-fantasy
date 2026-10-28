package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.league.RatingSettings;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.rating.PlayerRatingData;
import com.buddyfantasy.rating.RatingWindowClosed;
import com.buddyfantasy.rating.RatingsCorrected;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainEvent;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.buddyfantasy.datadefinition.Position.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rating window of a match: opening with frozen parameters, the deadline on the server's clock, the close and
 * the corrections of the sheet before and after it (RF-16, RF-17, RF-22, RF-23).
 */
class RatingWindowTests {

    static final RatingSettings DEFAULTS = TestRatingSettings.defaults();
    static final UUID LEAGUE = UUID.randomUUID();
    static final UUID MATCH = UUID.randomUUID();
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant CLOSED = KICKOFF.plus(Duration.ofHours(3));
    static final Instant CLOSES = CLOSED.plus(Duration.ofHours(DEFAULTS.windowHours()));
    static final Instant AFTER = CLOSES.plus(Duration.ofMinutes(1));

    final UUID keeper = UUID.randomUUID();
    final UUID striker = UUID.randomUUID();
    final UUID substitute = UUID.randomUUID();

    static MatchSchedule match() {
        return new MatchSchedule(MATCH, LEAGUE, 4, KICKOFF, true);
    }

    List<SheetPlayer> sheet() {
        return List.of(new SheetPlayer(keeper, GOALKEEPER), new SheetPlayer(striker, FORWARD));
    }

    RatingWindow open(RatingSettings settings) {
        return RatingWindow.open(match(), sheet(), settings, CLOSED);
    }

    static <T extends DomainEvent> T onlyEvent(RatingWindow window, Class<T> type) {
        List<DomainEvent> events = window.pullEvents();
        assertThat(events).singleElement().isInstanceOf(type);
        return type.cast(events.getFirst());
    }

    @Test
    @DisplayName("A closed match opens its window with the sheet and closes windowHours later")
    void aClosedMatchOpensItsWindowWithTheSheet() {
        RatingWindow window = open(DEFAULTS);

        assertThat(window.matchId()).isEqualTo(MATCH);
        assertThat(window.leagueId()).isEqualTo(LEAGUE);
        assertThat(window.matchday()).isEqualTo(4);
        assertThat(window.kickoffAt()).isEqualTo(KICKOFF);
        assertThat(window.openedAt()).isEqualTo(CLOSED);
        assertThat(window.closesAt()).isEqualTo(CLOSED.plus(Duration.ofHours(24)));
        assertThat(window.closed()).isFalse();
        assertThat(window.rules()).isEqualTo(RatingRules.from(DEFAULTS));
        assertThat(window.players()).containsExactlyElementsOf(sheet());
        assertThat(window.sheetAt()).isEqualTo(CLOSED);
        assertThat(window.results()).isEmpty();
        assertThat(window.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("Only a closed match opens a window, and its sheet has each player once")
    void onlyAClosedMatchOpensAWindow() {
        assertThatThrownBy(() -> RatingWindow.open(new MatchSchedule(MATCH, LEAGUE, 4, KICKOFF, false), sheet(),
                DEFAULTS, CLOSED)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> RatingWindow.open(match(),
                List.of(new SheetPlayer(keeper, GOALKEEPER), new SheetPlayer(keeper, FORWARD)), DEFAULTS, CLOSED))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("The window is open just before closesAt and closed from closesAt, even before the job runs")
    void theWindowIsOpenJustBeforeClosesAtAndClosedFromIt() {
        RatingWindow window = open(DEFAULTS);

        assertThatCode(() -> window.requireOpen(CLOSES.minusMillis(1))).doesNotThrowAnyException();
        assertThat(window.isOpen(CLOSES.minusMillis(1))).isTrue();
        assertThat(window.isOpen(CLOSES)).isFalse();
        assertThatThrownBy(() -> window.requireOpen(CLOSES))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).code())
                .isEqualTo(RatingError.RATING_WINDOW_CLOSED);
    }

    @Test
    @DisplayName("Closing before closesAt does nothing")
    void closingBeforeClosesAtDoesNothing() {
        RatingWindow window = open(DEFAULTS);

        assertThat(window.close(Map.of(keeper, List.of(4)), CLOSES.minusMillis(1))).isFalse();

        assertThat(window.closed()).isFalse();
        assertThat(window.results()).isEmpty();
        assertThat(window.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("Closing computes every player's result and publishes RatingWindowClosed once")
    void closingComputesEveryPlayersResultAndPublishesRatingWindowClosedOnce() {
        RatingWindow window = open(DEFAULTS);

        assertThat(window.close(Map.of(keeper, List.of(3, 4), UUID.randomUUID(), List.of(4)), AFTER)).isTrue();

        assertThat(window.closed()).isTrue();
        assertThat(window.closedAt()).isEqualTo(AFTER);
        assertThatThrownBy(() -> window.requireOpen(CLOSES.minusMillis(1))).isInstanceOf(ConflictException.class);
        assertThat(window.results()).containsOnly(
                Map.entry(keeper, new PlayerRatingResult(2, new BigDecimal("3.50"), 12)),
                Map.entry(striker, new PlayerRatingResult(0, null, 0)));
        RatingWindowClosed event = onlyEvent(window, RatingWindowClosed.class);
        assertThat(event.leagueId()).isEqualTo(LEAGUE);
        assertThat(event.matchId()).isEqualTo(MATCH);
        assertThat(event.occurredAt()).isEqualTo(AFTER);
        assertThat(event.players()).containsExactly(new PlayerRatingData(keeper, 2, new BigDecimal("3.50"), 12),
                new PlayerRatingData(striker, 0, null, 0));

        assertThat(window.close(Map.of(keeper, List.of(0)), AFTER.plusSeconds(60))).isFalse();
        assertThat(window.closedAt()).isEqualTo(AFTER);
        assertThat(window.results().get(keeper).ratingPoints()).isEqualTo(12);
        assertThat(window.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("The parameters frozen at the opening are used at the close and in corrections")
    void theParametersFrozenAtTheOpeningAreUsedAtTheCloseAndInCorrections() {
        RatingSettings edited = TestRatingSettings.with(TestRatingSettings.decimal(), "2", "1", 2, "6");
        RatingWindow window = open(edited);

        window.close(Map.of(keeper, List.of(7, 8), striker, List.of(9)), AFTER);
        window.applySheet(List.of(new SheetPlayer(keeper, GOALKEEPER), new SheetPlayer(substitute, DEFENDER)),
                AFTER.plusSeconds(60), AFTER.plusSeconds(61));

        assertThat(window.rules()).isEqualTo(RatingRules.from(edited));
        assertThat(window.results()).containsOnly(
                Map.entry(keeper, new PlayerRatingResult(2, new BigDecimal("7.50"), 16)),
                Map.entry(substitute, new PlayerRatingResult(0, null, 13)));
    }

    @Test
    @DisplayName("A correction with the window open replaces the players and the close uses them, without events")
    void aCorrectionWithTheWindowOpenReplacesThePlayers() {
        RatingWindow window = open(DEFAULTS);
        List<SheetPlayer> corrected = List.of(new SheetPlayer(keeper, DEFENDER), new SheetPlayer(substitute, FORWARD));

        assertThat(window.applySheet(corrected, CLOSED.plusSeconds(60), CLOSED.plusSeconds(61))).isTrue();

        assertThat(window.players()).containsExactlyElementsOf(corrected);
        assertThat(window.sheetAt()).isEqualTo(CLOSED.plusSeconds(60));
        assertThat(window.hasPlayer(striker)).isFalse();
        assertThat(window.hasPlayer(substitute)).isTrue();
        assertThat(window.pullEvents()).isEmpty();
        window.close(Map.of(substitute, List.of(4)), AFTER);
        assertThat(onlyEvent(window, RatingWindowClosed.class).players()).containsExactly(
                new PlayerRatingData(keeper, 0, null, 0),
                new PlayerRatingData(substitute, 1, new BigDecimal("4.00"), 14));
    }

    @Test
    @DisplayName("An older or repeated sheet changes nothing, with the window open or closed")
    void anOlderOrRepeatedSheetChangesNothing() {
        RatingWindow window = open(DEFAULTS);
        List<SheetPlayer> corrected = List.of(new SheetPlayer(keeper, GOALKEEPER));
        window.applySheet(corrected, CLOSED.plusSeconds(60), CLOSED.plusSeconds(61));

        assertThat(window.applySheet(sheet(), CLOSED, AFTER)).isFalse();
        assertThat(window.applySheet(sheet(), CLOSED.plusSeconds(60), AFTER)).isFalse();
        window.close(Map.of(), AFTER);
        window.pullEvents();
        assertThat(window.applySheet(sheet(), CLOSED, AFTER.plusSeconds(1))).isFalse();

        assertThat(window.players()).containsExactlyElementsOf(corrected);
        assertThat(window.results()).containsOnlyKeys(keeper);
        assertThat(window.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("A correction with the window closed keeps the results, adds players unrated and drops the removed")
    void aCorrectionWithTheWindowClosedPublishesRatingsCorrected() {
        RatingWindow window = open(DEFAULTS);
        window.close(Map.of(keeper, List.of(4), striker, List.of(2)), AFTER);
        window.pullEvents();
        Instant correctedAt = AFTER.plusSeconds(60);
        Instant appliedAt = AFTER.plusSeconds(90);
        List<SheetPlayer> corrected =
                List.of(new SheetPlayer(keeper, GOALKEEPER), new SheetPlayer(substitute, DEFENDER));

        assertThat(window.applySheet(corrected, correctedAt, appliedAt)).isTrue();

        assertThat(window.closedAt()).isEqualTo(AFTER);
        assertThat(window.sheetAt()).isEqualTo(correctedAt);
        assertThat(window.results()).containsOnly(
                Map.entry(keeper, new PlayerRatingResult(1, new BigDecimal("4.00"), 14)),
                Map.entry(substitute, new PlayerRatingResult(0, null, 0)));
        RatingsCorrected event = onlyEvent(window, RatingsCorrected.class);
        assertThat(event.leagueId()).isEqualTo(LEAGUE);
        assertThat(event.matchId()).isEqualTo(MATCH);
        assertThat(event.occurredAt()).as("after the close, whenever the correction happened").isEqualTo(appliedAt);
        assertThat(event.players()).containsExactly(new PlayerRatingData(keeper, 1, new BigDecimal("4.00"), 14),
                new PlayerRatingData(substitute, 0, null, 0));
    }

    @Test
    @DisplayName("A correction with the window closed that changes no result publishes nothing")
    void aCorrectionWithTheWindowClosedThatChangesNoResultPublishesNothing() {
        RatingWindow window = open(DEFAULTS);
        window.close(Map.of(keeper, List.of(4)), AFTER);
        window.pullEvents();
        List<SheetPlayer> samePlayersOtherPositions =
                List.of(new SheetPlayer(striker, MIDFIELDER), new SheetPlayer(keeper, DEFENDER));

        assertThat(window.applySheet(samePlayersOtherPositions, AFTER.plusSeconds(60), AFTER.plusSeconds(61)))
                .isTrue();

        assertThat(window.players()).containsExactlyElementsOf(samePlayersOtherPositions);
        assertThat(window.results()).containsOnlyKeys(keeper, striker);
        assertThat(window.pullEvents()).isEmpty();
    }
}
