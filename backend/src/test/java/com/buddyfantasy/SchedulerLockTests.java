package com.buddyfantasy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.LineupsLocked;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.infrastructure.ResubmitIncompleteEventsJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RNF-03: the scheduled jobs run under a ShedLock lock in the {@code shedlock} table, so with several instances only
 * one runs each job at a time. {@link SchedulingArchitectureTests} checks that every job carries the lock.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupLockTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class SchedulerLockTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    /** Long after the lock time of {@link #KICKOFF}: the match is due. */
    static final Instant DUE = KICKOFF.minus(Duration.ofHours(1));
    /** Long ago, in the database clock that ShedLock uses. */
    static final Timestamp LONG_AGO = Timestamp.from(Instant.parse("2000-01-01T00:00:00Z"));

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    LockDueLineupsJob lockJob;

    @Autowired
    ResubmitIncompleteEventsJob resubmitJob;

    @Autowired
    RecordedEvents events;

    UUID match;

    @BeforeEach
    void setUp() {
        clock.now = Instant.parse("2026-01-10T10:00:00Z");
        UUID admin = fixtures.user("admin");
        TestLeague peña = fixtures.league(admin);
        List<UUID> squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD,
                FORWARD);
        bus.dispatch(new SaveLineupCommand(admin, peña.id(), squad));
        match = fixtures.schedule(peña, 1, KICKOFF);
        clock.now = DUE;
    }

    /**
     * Takes the lock of {@code job} as another instance would, until an hour from now. Its columns are UTC
     * {@code timestamp} without time zone, as ShedLock writes them, not in the session's time zone.
     */
    private void lockedByAnotherInstance(String job) {
        jdbc.update("""
                insert into shedlock (name, lock_until, locked_at, locked_by)
                values (?, timezone('utc', now()) + interval '1 hour', timezone('utc', now()), 'other-instance')
                on conflict (name) do update
                set lock_until = excluded.lock_until, locked_at = excluded.locked_at, locked_by = excluded.locked_by""",
                job);
    }

    private List<UUID> lockedMatches() {
        return jdbc.queryForList("select match_id from lineup_match_lock", UUID.class);
    }

    @Test
    @DisplayName("Every job runs under its lock")
    void everyJobRunsUnderItsLock() {
        // ShedLock never deletes its rows: they may be there from an earlier test
        jdbc.update("update shedlock set locked_at = ?, lock_until = ?", LONG_AGO, LONG_AGO);

        lockJob.lockDueLineups();
        resubmitJob.resubmitIncompleteEvents();

        assertThat(jdbc.queryForList("select name from shedlock where locked_at > ?", String.class, LONG_AGO))
                .contains("LockDueLineupsJob", "ResubmitIncompleteEventsJob");
    }

    @Test
    @DisplayName("The lock job does nothing while another instance holds its lock")
    void theLockJobDoesNothingWhileAnotherInstanceHoldsItsLock() {
        lockedByAnotherInstance("LockDueLineupsJob");

        lockJob.lockDueLineups();

        assertThat(lockedMatches()).isEmpty();
        assertThat(events.stream(LineupsLocked.class)).isEmpty();

        // once released, the next run locks the match
        jdbc.update("update shedlock set lock_until = timezone('utc', now()) where name = 'LockDueLineupsJob'");
        lockJob.lockDueLineups();

        assertThat(lockedMatches()).containsExactly(match);
        assertThat(events.stream(LineupsLocked.class)).hasSize(1);
    }
}
