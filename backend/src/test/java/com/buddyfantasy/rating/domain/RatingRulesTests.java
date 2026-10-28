package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The rating parameters frozen in a window and the format of a vote (RF-19, RF-22, RF-23). */
class RatingRulesTests {

    static RatingRules rules(RatingFormat format, String factor, int minVotes, String defaultRating) {
        return new RatingRules(format, new BigDecimal(factor), BigDecimal.ZERO, minVotes,
                defaultRating == null ? null : new BigDecimal(defaultRating));
    }

    @Test
    @DisplayName("A vote in spades is a whole value from 0 to 4")
    void aVoteInSpadesIsFromZeroToFour() {
        RatingRules spades = rules(RatingFormat.SPADES, "4", 1, null);

        assertThatCode(() -> spades.requireValidVote(0)).doesNotThrowAnyException();
        assertThatCode(() -> spades.requireValidVote(4)).doesNotThrowAnyException();
        assertThatThrownBy(() -> spades.requireValidVote(5))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo(RatingError.INVALID_RATING);
        assertThatThrownBy(() -> spades.requireValidVote(-1))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo(RatingError.INVALID_RATING);
    }

    @Test
    @DisplayName("A vote in decimal points is a whole value from 0 to 10")
    void aVoteInDecimalPointsIsFromZeroToTen() {
        RatingRules decimal = rules(RatingFormat.DECIMAL, "1", 1, null);

        assertThatCode(() -> decimal.requireValidVote(0)).doesNotThrowAnyException();
        assertThatCode(() -> decimal.requireValidVote(10)).doesNotThrowAnyException();
        assertThatThrownBy(() -> decimal.requireValidVote(11))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo(RatingError.INVALID_RATING);
    }

    @Test
    @DisplayName("The factor is positive, the minimum of votes at least 1 and the default rating within the format")
    void theRulesAreValidated() {
        assertThatThrownBy(() -> rules(RatingFormat.SPADES, "0", 1, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> rules(RatingFormat.SPADES, "-1", 1, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> rules(RatingFormat.SPADES, "4", 0, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> rules(RatingFormat.SPADES, "4", 1, "4.5"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> rules(RatingFormat.SPADES, "4", 1, "-0.5"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RatingRules(RatingFormat.SPADES, BigDecimal.ONE, null, 1, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RatingRules(null, BigDecimal.ONE, BigDecimal.ZERO, 1, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatCode(() -> rules(RatingFormat.DECIMAL, "0.5", 3, "10")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The rules are taken from the league's rating settings")
    void theRulesAreTakenFromTheLeaguesRatingSettings() {
        RatingRules rules = RatingRules.from(TestRatingSettings.with(TestRatingSettings.decimal(), "2", "1", 3, "5"));

        assertThat(rules).isEqualTo(new RatingRules(RatingFormat.DECIMAL, new BigDecimal("2"), new BigDecimal("1"), 3,
                new BigDecimal("5")));
    }
}
