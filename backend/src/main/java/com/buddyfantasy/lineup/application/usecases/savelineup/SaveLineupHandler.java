package com.buddyfantasy.lineup.application.usecases.savelineup;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.league.LineupSettings;
import com.buddyfantasy.lineup.application.port.out.LineupVersionRepository;
import com.buddyfantasy.lineup.application.port.out.PlayerCatalog;
import com.buddyfantasy.lineup.application.usecases.getmylineup.MyLineupView;
import com.buddyfantasy.lineup.domain.LineupPlayer;
import com.buddyfantasy.lineup.domain.LineupRules;
import com.buddyfantasy.lineup.domain.LineupTimeline;
import com.buddyfantasy.lineup.domain.LineupVersion;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.DomainValidationException;
import org.springframework.stereotype.Component;

/**
 * The new lineup must be valid with the current squad, values and parameters. Changes are counted against the
 * lineup locked in the last locked match, so the deadline is the server clock's. Saving the current lineup again
 * creates no version.
 */
@Component
class SaveLineupHandler implements CommandHandler<SaveLineupCommand, MyLineupView> {

    private final LeagueAccess access;
    private final LeagueSettingsProvider leagueSettings;
    private final MatchApi matches;
    private final LineupVersionRepository versions;
    private final PlayerCatalog players;
    private final Clock clock;

    SaveLineupHandler(LeagueAccess access, LeagueSettingsProvider leagueSettings, MatchApi matches,
                      LineupVersionRepository versions, PlayerCatalog players, Clock clock) {
        this.access = access;
        this.leagueSettings = leagueSettings;
        this.matches = matches;
        this.versions = versions;
        this.players = players;
        this.clock = clock;
    }

    @Override
    public MyLineupView handle(SaveLineupCommand command) {
        access.requireMember(command.userId(), command.leagueId());
        // not contains(null): immutable lists throw on it
        if (command.playerIds() == null || command.playerIds().stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("Players are required");
        }
        LineupSettings settings = leagueSettings.lineupSettings(command.leagueId());
        LineupRules rules = new LineupRules(settings);
        // before asking squad for the players, so a malformed lineup costs nothing
        rules.requireShape(command.playerIds());

        List<MatchSchedule> calendar = matches.findMatches(command.leagueId());
        List<LineupVersion> saved = new ArrayList<>(
                versions.findByLeagueIdAndUserId(command.leagueId(), command.userId()));
        LineupTimeline timeline = new LineupTimeline(calendar, settings.lockHours(), saved);
        Instant now = clock.instant();
        Optional<LineupVersion> baseline = timeline.baseline(now);
        Set<UUID> playerIds = new HashSet<>(command.playerIds());
        baseline.ifPresent(version -> playerIds.addAll(version.playerIds()));
        Map<UUID, LineupPlayer> lineupPlayers = players.findPlayers(command.leagueId(), playerIds);

        rules.requireValid(command.playerIds(), lineupPlayers);
        boolean unchanged = timeline.current().filter(version -> version.hasPlayers(command.playerIds())).isPresent();
        if (!unchanged) {
            rules.requireWithinMaxChanges(baseline, command.playerIds(), lineupPlayers);
            LineupVersion version = LineupVersion.save(command.leagueId(), command.userId(), command.playerIds(), now);
            versions.save(version);
            saved.add(version);
            timeline = new LineupTimeline(calendar, settings.lockHours(), saved);
        }
        return MyLineupView.from(timeline, settings, lineupPlayers, now);
    }
}
