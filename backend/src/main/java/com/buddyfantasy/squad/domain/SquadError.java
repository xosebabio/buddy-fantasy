package com.buddyfantasy.squad.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/**
 * Why a player or their photo cannot be saved. {@link #SHIRT_NUMBER_TAKEN} and {@link #PLAYER_RETIRED} are 409; the
 * rest, 400.
 */
public enum SquadError implements ErrorCode {
    /** Another active player of the league has that shirt number. */
    SHIRT_NUMBER_TAKEN,
    /** A retired player cannot be edited nor get a photo. */
    PLAYER_RETIRED,
    /** Blank or longer than {@code squad.limits.name-max-length}. */
    INVALID_PLAYER_NAME,
    /** Outside {@code squad.limits.shirt-number-min} to {@code squad.limits.shirt-number-max}. */
    INVALID_SHIRT_NUMBER,
    /** Not positive, or with more digits than stored. */
    INVALID_INITIAL_VALUE,
    /** Not {@code image/webp}. */
    INVALID_PHOTO_FORMAT,
    /** Empty or larger than {@code squad.limits.photo-max-size}. */
    INVALID_PHOTO_SIZE,
    /** Nothing was uploaded under the photo key. */
    PHOTO_NOT_UPLOADED
}
