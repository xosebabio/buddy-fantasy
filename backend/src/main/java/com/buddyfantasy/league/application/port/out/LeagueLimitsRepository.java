package com.buddyfantasy.league.application.port.out;

import com.buddyfantasy.league.domain.LeagueLimits;

/** Configured business limits of the league module. Never hard-coded: they come from configuration. */
public interface LeagueLimitsRepository {

    LeagueLimits get();
}
