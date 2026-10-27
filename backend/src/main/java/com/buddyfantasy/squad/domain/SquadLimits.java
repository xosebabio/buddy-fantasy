package com.buddyfantasy.squad.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Business limits of the squad module ({@code squad.limits}). Checked on every write, never when loading stored
 * data, so lowering a limit does not break existing players.
 */
public record SquadLimits(int nameMaxLength, int shirtNumberMin, int shirtNumberMax, long photoMaxSizeBytes) {

    public SquadLimits {
        if (nameMaxLength < 1) {
            throw new DomainValidationException("squad.limits.name-max-length must be >= 1");
        }
        if (shirtNumberMin < ShirtNumber.MIN || shirtNumberMax > ShirtNumber.MAX || shirtNumberMin > shirtNumberMax) {
            throw new DomainValidationException("squad.limits.shirt-number-min/max must satisfy "
                    + ShirtNumber.MIN + " <= min <= max <= " + ShirtNumber.MAX);
        }
        if (photoMaxSizeBytes < 1) {
            throw new DomainValidationException("squad.limits.photo-max-size must be >= 1 byte");
        }
    }

    public void requireValidName(String name) {
        if (name == null || name.isBlank() || name.length() > nameMaxLength) {
            throw new DomainValidationException("Player name must be 1-" + nameMaxLength + " characters");
        }
    }

    /** {@code null} (no shirt number) is always valid. */
    public void requireValid(ShirtNumber shirtNumber) {
        if (shirtNumber != null && (shirtNumber.value() < shirtNumberMin || shirtNumber.value() > shirtNumberMax)) {
            throw new DomainValidationException("Shirt number must be " + shirtNumberMin + "-" + shirtNumberMax);
        }
    }

    public void requirePhotoSize(long sizeBytes) {
        if (sizeBytes <= 0 || sizeBytes > photoMaxSizeBytes) {
            throw new DomainValidationException("Player photos must be 1-" + photoMaxSizeBytes + " bytes");
        }
    }
}
