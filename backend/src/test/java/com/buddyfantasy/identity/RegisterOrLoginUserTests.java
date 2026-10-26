package com.buddyfantasy.identity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUser;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({PostgresTestConfig.class, RegisterOrLoginUserTests.FixedClockConfig.class})
class RegisterOrLoginUserTests {

    static final Instant T0 = Instant.parse("2026-01-10T10:00:00Z");

    static class MutableClock extends Clock {
        Instant now = T0;

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    CommandBus bus;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("delete from identity_user");
        clock.now = T0;
    }

    private static RegisterOrLoginUser login(String sub, String email) {
        return new RegisterOrLoginUser(sub, email, "Name " + sub, "http://avatar/" + sub);
    }

    private Instant column(UserId id, String column) {
        return jdbc.queryForObject("select " + column + " from identity_user where id = ?",
                java.sql.Timestamp.class, id.value()).toInstant();
    }

    @Test
    void firstLoginCreatesTheUserWithClockTimestamps() {
        UserId id = bus.dispatch(login("sub-1", "a@x.com"));

        assertThat(jdbc.queryForObject("select count(*) from identity_user", Integer.class)).isEqualTo(1);
        assertThat(column(id, "created_at")).isEqualTo(T0);
        assertThat(column(id, "last_login_at")).isEqualTo(T0);
    }

    @Test
    void secondLoginWithSameSubDoesNotDuplicateAndOnlyUpdatesLastLogin() {
        UserId first = bus.dispatch(login("sub-1", "a@x.com"));
        clock.now = T0.plusSeconds(3600);

        UserId second = bus.dispatch(new RegisterOrLoginUser("sub-1", "other@x.com", "Other name", null));

        assertThat(second).isEqualTo(first);
        assertThat(jdbc.queryForObject("select count(*) from identity_user", Integer.class)).isEqualTo(1);
        assertThat(column(first, "created_at")).isEqualTo(T0);
        assertThat(column(first, "last_login_at")).isEqualTo(T0.plusSeconds(3600));
        assertThat(jdbc.queryForObject("select email from identity_user where id = ?", String.class, first.value()))
                .isEqualTo("a@x.com");
        assertThat(jdbc.queryForObject("select display_name from identity_user where id = ?", String.class,
                first.value())).isEqualTo("Name sub-1");
    }

    @Test
    void differentSubWithSameEmailCreatesAnotherUser() {
        UserId a = bus.dispatch(login("sub-1", "same@x.com"));
        UserId b = bus.dispatch(login("sub-2", "same@x.com"));

        assertThat(a).isNotEqualTo(b);
        assertThat(jdbc.queryForObject("select count(*) from identity_user", Integer.class)).isEqualTo(2);
    }
}
