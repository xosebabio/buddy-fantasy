package com.buddyfantasy.datadefinition;

/** How managers rate a player (RF-19): whole values from 0 to {@code maxRating}. */
public enum RatingFormat {
    SPADES(4),
    DECIMAL(10);

    private final int maxRating;

    RatingFormat(int maxRating) {
        this.maxRating = maxRating;
    }

    public int maxRating() {
        return maxRating;
    }
}
