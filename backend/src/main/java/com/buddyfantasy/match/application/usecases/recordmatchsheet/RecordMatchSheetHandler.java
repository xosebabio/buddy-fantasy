package com.buddyfantasy.match.application.usecases.recordmatchsheet;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.match.application.port.out.MatchAuditEntry;
import com.buddyfantasy.match.application.port.out.MatchAuditLog;
import com.buddyfantasy.match.application.port.out.MatchLimitsRepository;
import com.buddyfantasy.match.application.port.out.MatchRepository;
import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchAuditAction;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.MatchLimits;
import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchSheet;
import com.buddyfantasy.match.domain.MatchSheetWarning;
import com.buddyfantasy.match.domain.PlayerPerformance;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.SquadApi;
import com.buddyfantasy.squad.SquadPlayer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Every player must belong to the league. A player new to the sheet must be active and gets their current position
 * from {@code squad}; a player already in it keeps the stored position and stays even if retired since, so editing
 * the squad never changes a recorded sheet.
 */
@Component
class RecordMatchSheetHandler implements CommandHandler<RecordMatchSheetCommand, List<MatchSheetWarning>> {

    private final LeagueAccess access;
    private final MatchRepository matches;
    private final SquadApi squad;
    private final MatchAuditLog audit;
    private final MatchLimitsRepository limits;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    RecordMatchSheetHandler(LeagueAccess access, MatchRepository matches, SquadApi squad, MatchAuditLog audit,
                            MatchLimitsRepository limits, ApplicationEventPublisher events, Clock clock) {
        this.access = access;
        this.matches = matches;
        this.squad = squad;
        this.audit = audit;
        this.limits = limits;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public List<MatchSheetWarning> handle(RecordMatchSheetCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Match match = matches.findByLeagueIdAndId(command.leagueId(), new MatchId(command.matchId()))
                .orElseThrow(() -> new NotFoundException("Match " + command.matchId() + " not in league "
                        + command.leagueId()));
        MatchLimits matchLimits = limits.get();
        MatchSheet sheet = new MatchSheet(result(command), performances(command, match, matchLimits));
        Instant now = clock.instant();
        MatchSnapshot before = MatchSnapshot.of(match);
        match.recordSheet(sheet, now, matchLimits);
        MatchSnapshot after = MatchSnapshot.of(match);
        if (!after.equals(before)) {
            matches.save(match);
            MatchAuditAction action = match.isClosed() ? MatchAuditAction.CORRECTED : MatchAuditAction.SHEET_RECORDED;
            audit.append(new MatchAuditEntry(command.leagueId(), match.id().value(), command.userId(), action, now,
                    before, after));
            match.pullEvents().forEach(events::publishEvent);
        }
        return match.sheet().warnings();
    }

    private static MatchResult result(RecordMatchSheetCommand command) {
        if (command.goalsFor() == null && command.goalsAgainst() == null) {
            return null;
        }
        if (command.goalsFor() == null || command.goalsAgainst() == null) {
            throw new DomainValidationException("A result needs both goals for and goals against");
        }
        return new MatchResult(command.goalsFor(), command.goalsAgainst());
    }

    private List<PlayerPerformance> performances(RecordMatchSheetCommand command, Match match, MatchLimits limits) {
        if (command.performances() == null) {
            throw new DomainValidationException("Performances are required");
        }
        // before asking squad for the players, so an oversized sheet costs nothing
        limits.requireSheetSize(command.performances().size());
        if (command.performances().isEmpty()) {
            return List.of();
        }
        List<UUID> playerIds = command.performances().stream().map(PerformanceLine::playerId).toList();
        if (playerIds.contains(null)) {
            throw new DomainValidationException("Player is required");
        }
        Map<UUID, SquadPlayer> players = squad.findPlayers(command.leagueId(), playerIds).stream()
                .collect(Collectors.toMap(SquadPlayer::id, Function.identity()));
        Map<UUID, Position> recorded = match.sheet().performances().stream()
                .collect(Collectors.toMap(PlayerPerformance::playerId, PlayerPerformance::position));
        return command.performances().stream()
                .map(line -> toPerformance(line, position(line.playerId(), players, recorded)))
                .toList();
    }

    private static Position position(UUID playerId, Map<UUID, SquadPlayer> players, Map<UUID, Position> recorded) {
        SquadPlayer player = players.get(playerId);
        if (player == null) {
            throw new DomainValidationException("Player " + playerId + " is not in the league's squad");
        }
        Position kept = recorded.get(playerId);
        if (kept != null) {
            return kept;
        }
        if (!player.active()) {
            throw new DomainValidationException("Retired player " + playerId + " cannot be added to the sheet");
        }
        return player.position();
    }

    private static PlayerPerformance toPerformance(PerformanceLine line, Position position) {
        return new PlayerPerformance(line.playerId(), position, line.goals(), line.penaltyGoals(),
                line.penaltiesSaved(), line.penaltiesMissed(), line.assists(), line.ownGoals(), line.yellowCard(),
                line.doubleYellow(), line.redCard());
    }
}
