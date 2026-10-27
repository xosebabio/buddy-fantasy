package com.buddyfantasy.league.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Business limits of the league module ({@code league.limits}). Checked on every write, never when loading stored
 * data, so lowering a limit does not break existing leagues.
 */
public record LeagueLimits(int nameMaxLength, int lineupLockHoursMax) {

    public LeagueLimits {
        if (nameMaxLength < 1) {
            throw new DomainValidationException("league.limits.name-max-length must be >= 1");
        }
        if (lineupLockHoursMax < 0) {
            throw new DomainValidationException("league.limits.lineup-lock-hours-max must be >= 0");
        }
    }

    public void requireValidName(String name) {
        if (name == null || name.isBlank() || name.length() > nameMaxLength) {
            throw new DomainValidationException("League name must be 1-" + nameMaxLength + " characters");
        }
    }

    public void requireWithin(LeagueSettings settings) {
        if (settings.lineupLockHours() > lineupLockHoursMax) {
            throw new DomainValidationException("lineupLockHours must be between 0 and " + lineupLockHoursMax);
        }
    }
}
