package com.buddyfantasy.rating.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.league.RatingSettings;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.buddyfantasy.datadefinition.Position.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A member rates the players of a match while its window is open (RF-18 to RF-20, RF-41). */
class MyRatingsTests {

    static final RatingSettings DEFAULTS = TestRatingSettings.defaults();
    static final UUID MATCH = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant CLOSED = KICKOFF.plus(Duration.ofHours(3));
    static final Instant CLOSES = CLOSED.plus(Duration.ofHours(DEFAULTS.windowHours()));
    static final Instant NOW = CLOSED.plus(Duration.ofHours(1));

    final UUID keeper = UUID.randomUUID();
    final UUID striker = UUID.randomUUID();

    RatingWindow window(RatingSettings settings) {
        return RatingWindow.open(new MatchSchedule(MATCH, UUID.randomUUID(), 4, KICKOFF, true),
                List.of(new SheetPlayer(keeper, GOALKEEPER), new SheetPlayer(striker, FORWARD)), settings, CLOSED);
    }

    static MyRatings none() {
        return MyRatings.none(MATCH, USER);
    }

    static void assertInvalid(RatingError code, ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo(code);
    }

    static void assertConflict(RatingError code, ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).code())
                .isEqualTo(code);
    }

    @Test
    @DisplayName("A member rates some players of the sheet and leaves others unrated")
    void aMemberRatesSomePlayersOfTheSheet() {
        MyRatings mine = none().replace(List.of(new Vote(keeper, 3)), window(DEFAULTS), false, NOW);

        assertThat(mine).isEqualTo(new MyRatings(MATCH, USER, List.of(new Vote(keeper, 3))));
    }

    @Test
    @DisplayName("Saving replaces all the votes: changed values, new players and removed votes")
    void savingReplacesAllTheVotes() {
        RatingWindow window = window(DEFAULTS);
        MyRatings mine = none().replace(List.of(new Vote(keeper, 3)), window, false, NOW);

        MyRatings edited = mine.replace(List.of(new Vote(striker, 4)), window, false, NOW.plusSeconds(60));

        assertThat(edited.votes()).containsExactly(new Vote(striker, 4));
        assertThat(edited.replace(List.of(), window, false, NOW.plusSeconds(120)).votes()).isEmpty();
    }

    @Test
    @DisplayName("Votes are accepted just before closesAt and rejected from closesAt, with the job or without it")
    void votesAreAcceptedJustBeforeClosesAtAndRejectedFromIt() {
        RatingWindow window = window(DEFAULTS);
        List<Vote> votes = List.of(new Vote(keeper, 3));

        assertThat(none().replace(votes, window, false, CLOSES.minusMillis(1)).votes()).hasSize(1);
        assertConflict(RatingError.RATING_WINDOW_CLOSED, () -> none().replace(votes, window, false, CLOSES));
        window.close(Map.of(), CLOSES);
        assertConflict(RatingError.RATING_WINDOW_CLOSED,
                () -> none().replace(votes, window, false, CLOSES.minusMillis(1)));
    }

    @Test
    @DisplayName("An excluded member cannot rate the match, not even to remove his votes")
    void anExcludedMemberCannotRateTheMatch() {
        RatingWindow window = window(DEFAULTS);

        assertConflict(RatingError.RATER_EXCLUDED,
                () -> none().replace(List.of(new Vote(keeper, 3)), window, true, NOW));
        assertConflict(RatingError.RATER_EXCLUDED, () -> none().replace(List.of(), window, true, NOW));
    }

    @Test
    @DisplayName("Only the players of the sheet can be rated, each once")
    void onlyThePlayersOfTheSheetCanBeRatedEachOnce() {
        RatingWindow window = window(DEFAULTS);

        assertInvalid(RatingError.PLAYER_NOT_IN_SHEET,
                () -> none().replace(List.of(new Vote(UUID.randomUUID(), 3)), window, false, NOW));
        assertInvalid(RatingError.DUPLICATE_PLAYER,
                () -> none().replace(List.of(new Vote(keeper, 3), new Vote(keeper, 2)), window, false, NOW));
    }

    @Test
    @DisplayName("Votes follow the window's format: 0 to 4 spades or 0 to 10 points")
    void votesFollowTheWindowsFormat() {
        RatingWindow spades = window(DEFAULTS);
        RatingWindow decimal = window(TestRatingSettings.decimal());

        assertInvalid(RatingError.INVALID_RATING,
                () -> none().replace(List.of(new Vote(keeper, 5)), spades, false, NOW));
        assertInvalid(RatingError.INVALID_RATING,
                () -> none().replace(List.of(new Vote(keeper, -1)), spades, false, NOW));
        assertThat(none().replace(List.of(new Vote(keeper, 10), new Vote(striker, 0)), decimal, false, NOW).votes())
                .hasSize(2);
        assertInvalid(RatingError.INVALID_RATING,
                () -> none().replace(List.of(new Vote(keeper, 11)), decimal, false, NOW));
    }

    @Test
    @DisplayName("The votes of a match cannot be saved with another match's window")
    void theVotesOfAMatchCannotBeSavedWithAnotherMatchsWindow() {
        RatingWindow other = RatingWindow.open(new MatchSchedule(UUID.randomUUID(), UUID.randomUUID(), 4, KICKOFF,
                true), List.of(new SheetPlayer(keeper, GOALKEEPER)), DEFAULTS, CLOSED);

        assertThatThrownBy(() -> none().replace(List.of(), other, false, NOW))
                .isInstanceOf(DomainValidationException.class);
    }
}
