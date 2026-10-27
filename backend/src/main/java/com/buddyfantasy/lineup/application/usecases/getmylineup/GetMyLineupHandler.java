package com.buddyfantasy.lineup.application.usecases.getmylineup;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.application.port.out.LineupVersionRepository;
import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.lineup.application.port.out.PlayerCatalog;
import com.buddyfantasy.lineup.domain.LineupTimeline;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

/** Values, statuses and parameters are the current ones, so the warnings reflect what changed since saving. */
@Component
class GetMyLineupHandler implements QueryHandler<GetMyLineupQuery, MyLineupView> {

    private final LeagueAccess access;
    private final LeagueSettingsProvider leagueSettings;
    private final MatchApi matches;
    private final LineupVersionRepository versions;
    private final MatchLineupLockRepository locks;
    private final PlayerCatalog players;
    private final Clock clock;

    GetMyLineupHandler(LeagueAccess access, LeagueSettingsProvider leagueSettings, MatchApi matches,
                       LineupVersionRepository versions, MatchLineupLockRepository locks,
                       PlayerCatalog players, Clock clock) {
        this.access = access;
        this.leagueSettings = leagueSettings;
        this.matches = matches;
        this.versions = versions;
        this.locks = locks;
        this.players = players;
        this.clock = clock;
    }

    @Override
    public MyLineupView handle(GetMyLineupQuery query) {
        Instant memberSince = access.requireMemberSince(query.userId(), query.leagueId());
        LineupSettings settings = leagueSettings.lineupSettings(query.leagueId());
        LineupTimeline timeline = new LineupTimeline(matches.findMatches(query.leagueId()), settings.lockHours(),
                locks.findLockAtsByLeagueId(query.leagueId()),
                versions.findByLeagueIdAndUserId(query.leagueId(), query.userId()), memberSince);
        Instant now = clock.instant();
        Set<UUID> playerIds = new HashSet<>();
        timeline.current().ifPresent(version -> playerIds.addAll(version.playerIds()));
        timeline.baseline(now).ifPresent(version -> playerIds.addAll(version.playerIds()));
        return MyLineupView.from(timeline, settings, players.findPlayers(query.leagueId(), playerIds), now);
    }
}
