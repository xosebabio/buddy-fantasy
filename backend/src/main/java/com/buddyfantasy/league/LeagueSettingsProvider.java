package com.buddyfantasy.league;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Public API of the league module: read-only access to a league's parameters for other modules. */
public interface LeagueSettingsProvider {

    /** Throws {@link com.buddyfantasy.shared.domain.NotFoundException} (404) if the league does not exist. */
    LineupSettings lineupSettings(UUID leagueId);

    /** One query for all ids. Unknown leagues are absent from the result. */
    Map<UUID, LineupSettings> lineupSettings(Collection<UUID> leagueIds);
}
