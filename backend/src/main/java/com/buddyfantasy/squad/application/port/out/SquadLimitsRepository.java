package com.buddyfantasy.squad.application.port.out;

import com.buddyfantasy.squad.domain.SquadLimits;

/** Configured business limits of the squad module. Never hard-coded: they come from configuration. */
public interface SquadLimitsRepository {

    SquadLimits get();
}
