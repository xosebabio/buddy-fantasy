package com.buddyfantasy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.LineupsLocked;
import com.buddyfantasy.lineup.LineupsUnlocked;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.match.MatchClosed;
import com.buddyfantasy.match.MatchCorrected;
import com.buddyfantasy.match.MatchDeleted;
import com.buddyfantasy.match.MatchRescheduled;
import com.buddyfantasy.match.application.usecases.deletematch.DeleteMatchCommand;
import com.buddyfantasy.match.application.usecases.updatematch.UpdateMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.rating.RatingWindowClosed;
import com.buddyfantasy.rating.RatingsCorrected;
import com.buddyfantasy.scoring.MatchdayScored;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.infrastructure.ResubmitIncompleteEventsJob;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.support.TransactionTemplate;

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RNF-03, decision D1: a module event whose listener fails stays incomplete and {@link ResubmitIncompleteEventsJob}
 * delivers it again once it is {@code shared.events.resubmit-job.min-age} old, by the server clock. Delivery is at
 * least once, so delivering again an event already processed must change nothing.
 */
@SpringBootTest
@Import({PostgresTestConfig.class, MutableClock.Config.class, EventResubmissionTests.ProbeLog.class,
        EventResubmissionTests.FlakyListener.class})
class EventResubmissionTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");

    /** Published only by these tests. */
    record Probe(UUID id) {
    }

    /** What {@link FlakyListener} went through; apart from it, since the listener is behind a proxy. */
    static class ProbeLog {

        /** Deliveries left to fail. */
        final AtomicInteger failures = new AtomicInteger();
        final AtomicInteger attempts = new AtomicInteger();
        final List<UUID> handled = new CopyOnWriteArrayList<>();
    }

    /** Fails while {@link ProbeLog#failures} is above zero, as a listener hitting a transient error would. */
    static class FlakyListener {

        private final ProbeLog log;

        FlakyListener(ProbeLog log) {
            this.log = log;
        }

        @ApplicationModuleListener
        void on(Probe probe) {
            log.attempts.incrementAndGet();
            if (log.failures.getAndUpdate(left -> Math.max(left - 1, 0)) > 0) {
                throw new IllegalStateException("Simulated transient failure");
            }
            log.handled.add(probe.id());
        }
    }

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    MutableClock clock;

    @Autowired
    ResubmitIncompleteEventsJob job;

    @Autowired
    LockDueLineupsJob lockJob;

    @Autowired
    RecordedEvents events;

    @Autowired
    ProbeLog probes;

    @Value("${shared.events.resubmit-job.min-age}")
    Duration minAge;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        probes.failures.set(0);
        probes.attempts.set(0);
        probes.handled.clear();
    }

    /** Publishes {@code event} in a transaction, as a command handler does. */
    private void publish(Object event) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private int incompletePublications() {
        return jdbc.queryForObject("select count(*) from event_publication where completion_date is null",
                Integer.class);
    }

    /** Publishes a probe whose listener fails once, and waits for that failure. */
    private UUID probeFailingOnce() {
        probes.failures.set(1);
        UUID id = UUID.randomUUID();
        publish(new Probe(id));
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollDelay(Duration.ZERO)
                .until(() -> probes.attempts.get() == 1);
        assertThat(incompletePublications()).isOne();
        return id;
    }

    @Test
    @DisplayName("A failed listener keeps its publication and the job delivers it once it is old enough")
    void aFailedListenerKeepsItsPublicationAndTheJobDeliversItOnceItIsOld() {
        UUID id = probeFailingOnce();
        assertThat(probes.handled).isEmpty();

        clock.now = NOW.plus(minAge).plusSeconds(1);
        job.resubmitIncompleteEvents();
        Listeners.await(jdbc);

        assertThat(probes.attempts).hasValue(2);
        assertThat(probes.handled).containsExactly(id);
        assertThat(incompletePublications()).isZero();
    }

    @Test
    @DisplayName("A recent publication is not delivered again")
    void aRecentPublicationIsNotDeliveredAgain() {
        UUID id = probeFailingOnce();

        clock.now = NOW.plus(minAge).minusSeconds(1);
        job.resubmitIncompleteEvents();

        // a delivery would be asynchronous: give it time to show up
        Awaitility.await().during(Duration.ofMillis(300)).atMost(Duration.ofSeconds(1))
                .until(() -> probes.attempts.get() == 1);
        assertThat(incompletePublications()).isOne();

        clock.now = NOW.plus(minAge).plusSeconds(1);
        job.resubmitIncompleteEvents();
        Listeners.await(jdbc);

        assertThat(probes.handled).containsExactly(id);
    }

    @Test
    @DisplayName("A listener that keeps failing is retried on every run")
    void aListenerThatKeepsFailingIsRetriedOnEveryRun() {
        probes.failures.set(2);
        UUID id = UUID.randomUUID();
        publish(new Probe(id));
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> probes.attempts.get() == 1);

        clock.now = NOW.plus(minAge).plusSeconds(1);
        job.resubmitIncompleteEvents();
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> probes.attempts.get() == 2);
        assertThat(incompletePublications()).isOne();

        job.resubmitIncompleteEvents();
        Listeners.await(jdbc);

        assertThat(probes.attempts).hasValue(3);
        assertThat(probes.handled).containsExactly(id);
    }

    TestLeague peña;
    UUID admin;
    UUID match1;
    List<UUID> squad;

    /** A league whose admin has a lineup for match 1, and a match 2 after it. */
    private void leagueWithALineup() {
        admin = fixtures.user("admin");
        peña = fixtures.league(admin);
        squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD, FORWARD);
        bus.dispatch(new SaveLineupCommand(admin, peña.id(), squad));
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        fixtures.schedule(peña, 2, KICKOFF_2);
    }

    /**
     * Marks the publications of {@code eventType} incomplete, as if their listeners had failed, and runs the job: the
     * listeners of the lineup, scoring and rating modules must change no row nor publish anything more.
     */
    private void deliverAgain(Class<?> eventType) {
        Map<String, String> before = rows();
        List<Object> publishedBefore = events.all();
        int publications = jdbc.update("update event_publication set completion_date = null where event_type = ?",
                eventType.getName());
        assertThat(publications).as("publications of " + eventType.getSimpleName()).isPositive();

        clock.now = clock.now.plus(minAge).plusSeconds(1);
        job.resubmitIncompleteEvents();
        Listeners.await(jdbc);

        assertThat(rows()).isEqualTo(before);
        assertThat(events.all()).isEqualTo(publishedBefore);
    }

    /** Every row but the publications, whose completion date the delivery sets again. */
    private Map<String, String> rows() {
        return TableDigests.of(jdbc, Stream.concat(TableDigests.ALWAYS_CHANGING.stream(),
                Stream.of("event_publication")).toList());
    }

    @Test
    @DisplayName("Delivering MatchClosed again neither locks nor scores twice")
    void deliveringAgainMatchClosedDoesNotLockNorScoreTwice() {
        leagueWithALineup();
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        fixtures.closeWithSheet(peña, match1, 1, 0, played(squad.get(5), 1));
        fixtures.closeRatings(clock, match1);
        assertThat(events.stream(LineupsLocked.class)).hasSize(1);
        assertThat(events.stream(MatchdayScored.class)).hasSize(1);

        deliverAgain(MatchClosed.class);
    }

    /** A close delivered again after the correction must not bring back the sheet it closed with. */
    @Test
    @DisplayName("Delivering MatchCorrected or the close before it again neither recalculates nor undoes it")
    void deliveringAgainMatchCorrectedOrTheCloseBeforeItNeitherRecalculatesNorUndoesIt() {
        leagueWithALineup();
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        fixtures.closeWithSheet(peña, match1, 1, 0, played(squad.get(5), 1));
        fixtures.closeRatings(clock, match1);
        clock.now = clock.now.plus(Duration.ofHours(1));
        fixtures.sheet(peña, match1, 2, 0, played(squad.get(5), 2));
        Listeners.await(jdbc);
        assertThat(events.stream(MatchdayScored.class)).hasSize(2);

        deliverAgain(MatchCorrected.class);
        deliverAgain(MatchClosed.class);
    }

    /**
     * Neither reopens the window nor brings back the players the correction removed, with their votes, nor scores
     * twice.
     */
    @Test
    @DisplayName("Delivering the match events or RatingWindowClosed again after the window closed changes nothing")
    void deliveringAgainMatchCorrectedMatchClosedOrRatingWindowClosedAfterTheWindowClosedChangesNothing() {
        leagueWithALineup();
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        fixtures.closeWithSheet(peña, match1, 1, 0, played(squad.get(5), 1));
        clock.now = clock.now.plus(Duration.ofHours(1));
        fixtures.sheet(peña, match1, 1, 0, played(squad.get(6), 1));
        Listeners.await(jdbc);
        fixtures.closeRatings(clock, match1);
        assertThat(events.stream(RatingWindowClosed.class)).hasSize(1);
        assertThat(events.stream(MatchdayScored.class)).hasSize(1);

        deliverAgain(MatchCorrected.class);
        deliverAgain(MatchClosed.class);
        deliverAgain(RatingWindowClosed.class);
    }

    /** The window's result delivered again after the correction must not bring back the player it removed. */
    @Test
    @DisplayName("Delivering RatingsCorrected or the window close before it again neither recalculates nor undoes it")
    void deliveringAgainRatingsCorrectedOrTheRatingWindowClosedBeforeItNeitherRecalculatesNorUndoesIt() {
        leagueWithALineup();
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        fixtures.closeWithSheet(peña, match1, 1, 0, played(squad.get(5), 1));
        fixtures.closeRatings(clock, match1);
        clock.now = clock.now.plus(Duration.ofHours(1));
        fixtures.sheet(peña, match1, 1, 0, played(squad.get(6), 1));
        Listeners.await(jdbc);
        assertThat(events.stream(RatingsCorrected.class)).hasSize(1);

        deliverAgain(RatingsCorrected.class);
        deliverAgain(RatingWindowClosed.class);
    }

    @Test
    @DisplayName("Delivering LineupsLocked again does not score twice")
    void deliveringAgainLineupsLockedDoesNotScoreTwice() {
        leagueWithALineup();
        clock.now = KICKOFF_1.minus(Duration.ofHours(1));
        lockJob.lockDueLineups();
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        fixtures.closeWithSheet(peña, match1, 1, 0, played(squad.get(5), 1));
        fixtures.closeRatings(clock, match1);
        assertThat(events.stream(MatchdayScored.class)).hasSize(1);

        deliverAgain(LineupsLocked.class);
    }

    /** A lock delivered again after its unlock must not bring back the lineups it locked. */
    @Test
    @DisplayName("Delivering LineupsUnlocked or the lock before it again changes nothing")
    void deliveringAgainLineupsUnlockedOrTheLockBeforeItChangesNothing() {
        leagueWithALineup();
        clock.now = KICKOFF_1.minus(Duration.ofHours(1));
        lockJob.lockDueLineups();
        clock.now = clock.now.plus(Duration.ofMinutes(10));
        bus.dispatch(new UpdateMatchCommand(admin, peña.id(), match1, 1, "Rival",
                Venue.HOME, KICKOFF_1.plus(Duration.ofDays(2))));
        Listeners.await(jdbc);
        assertThat(jdbc.queryForObject("select count(*) from scoring_lineup", Integer.class)).isZero();

        deliverAgain(LineupsUnlocked.class);
        deliverAgain(LineupsLocked.class);
    }

    @Test
    @DisplayName("Delivering MatchRescheduled again does not unlock twice")
    void deliveringAgainMatchRescheduledDoesNotUnlockTwice() {
        leagueWithALineup();
        clock.now = KICKOFF_1.minus(Duration.ofHours(1));
        lockJob.lockDueLineups();
        bus.dispatch(new UpdateMatchCommand(admin, peña.id(), match1, 1, "Rival",
                Venue.HOME, KICKOFF_1.plus(Duration.ofDays(2))));
        Listeners.await(jdbc);
        assertThat(events.stream(LineupsUnlocked.class)).hasSize(1);

        deliverAgain(MatchRescheduled.class);
    }

    @Test
    @DisplayName("Delivering MatchDeleted again changes nothing")
    void deliveringAgainMatchDeletedChangesNothing() {
        leagueWithALineup();
        clock.now = KICKOFF_1.minus(Duration.ofHours(1));
        lockJob.lockDueLineups();
        bus.dispatch(new DeleteMatchCommand(admin, peña.id(), match1));
        Listeners.await(jdbc);
        assertThat(jdbc.queryForObject("select count(*) from lineup_match_lock", Integer.class)).isZero();

        deliverAgain(MatchDeleted.class);
    }
}
