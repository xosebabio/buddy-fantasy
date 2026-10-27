package com.buddyfantasy.match.application.port.out;

import com.buddyfantasy.match.domain.MatchLimits;

/** Configured business limits of the match module. Never hard-coded: they come from configuration. */
public interface MatchLimitsRepository {

    MatchLimits get();
}
