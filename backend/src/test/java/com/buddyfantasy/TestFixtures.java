package com.buddyfantasy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.getleaguesettings.GetLeagueSettingsQuery;
import com.buddyfantasy.league.application.usecases.getleaguesettings.LeagueSettingsView;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.league.application.usecases.updateleaguesettings.UpdateLeagueSettingsCommand;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.league.domain.ScoringRules;
import com.buddyfantasy.match.application.usecases.closematch.CloseMatchCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.RecordMatchSheetCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test data prepared through the real use cases of the {@link CommandBus}, so the tests do not repeat it nor write
 * rows by hand. Available in every Spring test (imported by {@link PostgresTestConfig}).
 */
@TestComponent
@RequiredArgsConstructor
public class TestFixtures {

    /** Initial value of the players created without one, in millions. */
    public static final BigDecimal PLAYER_VALUE = BigDecimal.TEN;

    private final CommandBus bus;
    private final JdbcTemplate jdbc;

    /** A league created by {@link #admin}; {@code invitationToken} is its initial link. */
    public record TestLeague(UUID id, UUID admin, String invitationToken) {
    }

    /** Logs a user in for the first time: email {@code <sub>@x.com}, name {@code Name <sub>}, avatar {@code http://pic/<sub>}. */
    public UUID user(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, "http://pic/" + sub))
                .value();
    }

    /** A league named "Peña" created by {@code admin}, with the parameters of {@code league.defaults}. */
    public TestLeague league(UUID admin) {
        return league(admin, "Peña");
    }

    public TestLeague league(UUID admin, String name) {
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(admin, name));
        return new TestLeague(created.id().value(), admin, created.invitationToken());
    }

    /** The users join {@code league} as managers through its invitation link. */
    public void join(TestLeague league, UUID... users) {
        for (UUID user : users) {
            bus.dispatch(new JoinLeagueCommand(user, league.invitationToken()));
        }
    }

    /** Changes some parameters of {@code league} through the admin's settings use case, keeping the rest. */
    public void settings(TestLeague league, Consumer<EditableSettings> change) {
        LeagueSettingsView current = bus.ask(new GetLeagueSettingsQuery(league.admin(), league.id()));
        EditableSettings settings = new EditableSettings(current.settings());
        change.accept(settings);
        bus.dispatch(new UpdateLeagueSettingsCommand(league.admin(), league.id(), settings.toSettings(),
                current.scoringRules()));
    }

    /** Sets {@code action} of {@code league}'s table of points to {@code points} in every position, keeping the rest. */
    public void scoringRule(TestLeague league, ScoringAction action, int points) {
        LeagueSettingsView current = bus.ask(new GetLeagueSettingsQuery(league.admin(), league.id()));
        Map<ScoringAction, Map<Position, Integer>> rules = new EnumMap<>(current.scoringRules().points());
        Map<Position, Integer> byPosition = new EnumMap<>(Position.class);
        for (Position position : Position.values()) {
            byPosition.put(position, points);
        }
        rules.put(action, byPosition);
        bus.dispatch(new UpdateLeagueSettingsCommand(league.admin(), league.id(), current.settings(),
                new ScoringRules(rules)));
    }

    /** A player named "Player", without shirt number, worth {@link #PLAYER_VALUE}. */
    public UUID player(TestLeague league, Position position) {
        return player(league, "Player", null, position, PLAYER_VALUE);
    }

    public UUID player(TestLeague league, String name, Integer shirtNumber, Position position) {
        return player(league, name, shirtNumber, position, PLAYER_VALUE);
    }

    public UUID player(TestLeague league, String name, Integer shirtNumber, Position position, BigDecimal value) {
        return bus.dispatch(new RegisterPlayerCommand(league.admin(), league.id(), name, shirtNumber, position, value))
                .value();
    }

    /** One {@link #player(TestLeague, Position)} per position, in this order. */
    public List<UUID> squad(TestLeague league, Position... positions) {
        return Arrays.stream(positions).map(position -> player(league, position)).toList();
    }

    /** A home match against "Rival {@code matchday}". */
    public UUID schedule(TestLeague league, int matchday, Instant kickoffAt) {
        return schedule(league, matchday, "Rival " + matchday, kickoffAt);
    }

    public UUID schedule(TestLeague league, int matchday, String opponent, Instant kickoffAt) {
        return bus.dispatch(new ScheduleMatchCommand(league.admin(), league.id(), matchday, opponent, Venue.HOME,
                kickoffAt)).value();
    }

    /** Records the match sheet as the admin, without closing the match. */
    public void sheet(TestLeague league, UUID matchId, int goalsFor, int goalsAgainst, PerformanceLine... lines) {
        bus.dispatch(new RecordMatchSheetCommand(league.admin(), league.id(), matchId, goalsFor, goalsAgainst,
                List.of(lines)));
    }

    /** Records the match sheet, closes the match and waits for the listeners of {@code MatchClosed}. */
    public void closeWithSheet(TestLeague league, UUID matchId, int goalsFor, int goalsAgainst,
                               PerformanceLine... lines) {
        sheet(league, matchId, goalsFor, goalsAgainst, lines);
        bus.dispatch(new CloseMatchCommand(league.admin(), league.id(), matchId));
        Listeners.await(jdbc);
    }

    /** A line of a player who played and scored {@code goals}, without any other stat. */
    public static PerformanceLine played(UUID playerId, int goals) {
        return new PerformanceLine(playerId, goals, 0, 0, 0, 0, 0, false, false, false);
    }

    /** {@link LeagueSettings} with its parameters as public fields, for {@link #settings}. */
    public static final class EditableSettings {

        public int ratingWindowHours;
        public RatingFormat ratingFormat;
        public BigDecimal ratingSpadesFactor;
        public BigDecimal ratingSpadesBase;
        public BigDecimal ratingDecimalFactor;
        public BigDecimal ratingDecimalBase;
        public int ratingMinVotes;
        public BigDecimal defaultRating;
        public int lineupLockHours;
        public int lineupMaxChanges;
        public int lineupSize;
        public boolean lineupFormationEnabled;
        public int lineupMinGoalkeepers;
        public int lineupMinDefenders;
        public int lineupMinMidfielders;
        public int lineupMinForwards;
        public boolean lineupBudgetEnabled;
        public BigDecimal lineupBudget;
        public BigDecimal marketK;
        public BigDecimal marketMaxChangePct;
        public BigDecimal marketMinValue;

        EditableSettings(LeagueSettings settings) {
            ratingWindowHours = settings.ratingWindowHours();
            ratingFormat = settings.ratingFormat();
            ratingSpadesFactor = settings.ratingSpadesFactor();
            ratingSpadesBase = settings.ratingSpadesBase();
            ratingDecimalFactor = settings.ratingDecimalFactor();
            ratingDecimalBase = settings.ratingDecimalBase();
            ratingMinVotes = settings.ratingMinVotes();
            defaultRating = settings.defaultRating();
            lineupLockHours = settings.lineupLockHours();
            lineupMaxChanges = settings.lineupMaxChanges();
            lineupSize = settings.lineupSize();
            lineupFormationEnabled = settings.lineupFormationEnabled();
            lineupMinGoalkeepers = settings.lineupMinGoalkeepers();
            lineupMinDefenders = settings.lineupMinDefenders();
            lineupMinMidfielders = settings.lineupMinMidfielders();
            lineupMinForwards = settings.lineupMinForwards();
            lineupBudgetEnabled = settings.lineupBudgetEnabled();
            lineupBudget = settings.lineupBudget();
            marketK = settings.marketK();
            marketMaxChangePct = settings.marketMaxChangePct();
            marketMinValue = settings.marketMinValue();
        }

        LeagueSettings toSettings() {
            return new LeagueSettings(ratingWindowHours, ratingFormat, ratingSpadesFactor, ratingSpadesBase,
                    ratingDecimalFactor, ratingDecimalBase, ratingMinVotes, defaultRating, lineupLockHours,
                    lineupMaxChanges, lineupSize, lineupFormationEnabled, lineupMinGoalkeepers, lineupMinDefenders,
                    lineupMinMidfielders, lineupMinForwards, lineupBudgetEnabled, lineupBudget, marketK,
                    marketMaxChangePct, marketMinValue);
        }
    }
}
