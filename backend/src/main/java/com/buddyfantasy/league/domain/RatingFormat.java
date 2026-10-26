package com.buddyfantasy.league.domain;

import java.math.BigDecimal;

public enum RatingFormat {
    SPADES(new BigDecimal("4")),
    DECIMAL(new BigDecimal("10"));

    private final BigDecimal maxRating;

    RatingFormat(BigDecimal maxRating) {
        this.maxRating = maxRating;
    }

    public BigDecimal maxRating() {
        return maxRating;
    }
}
