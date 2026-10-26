package com.buddyfantasy.shared;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.shared.application.Command;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.application.Query;
import com.buddyfantasy.shared.application.QueryHandler;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({PostgresTestConfig.class, CommandBusTests.Handlers.class})
class CommandBusTests {

    record Echo(@NotBlank String text) implements Command<String> {
    }

    record InTransaction() implements Command<Boolean> {
    }

    record ReadOnlyProbe() implements Query<Boolean> {
    }

    record InsertThenFail() implements Command<Void> {
    }

    static class EchoHandler implements CommandHandler<Echo, String> {

        @Override
        public String handle(Echo command) {
            return "echo:" + command.text();
        }
    }

    static class InTransactionHandler implements CommandHandler<InTransaction, Boolean> {

        @Override
        public Boolean handle(InTransaction command) {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }
    }

    static class ReadOnlyProbeHandler implements QueryHandler<ReadOnlyProbe, Boolean> {

        @Override
        public Boolean handle(ReadOnlyProbe query) {
            return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        }
    }

    static class InsertThenFailHandler implements CommandHandler<InsertThenFail, Void> {

        private final JdbcTemplate jdbc;

        InsertThenFailHandler(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public Void handle(InsertThenFail command) {
            jdbc.update("insert into shedlock values ('bus-test', now(), now(), 'test')");
            throw new IllegalStateException("boom");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Handlers {

        @Bean
        EchoHandler echoHandler() {
            return new EchoHandler();
        }

        @Bean
        InTransactionHandler inTransactionHandler() {
            return new InTransactionHandler();
        }

        @Bean
        ReadOnlyProbeHandler readOnlyProbeHandler() {
            return new ReadOnlyProbeHandler();
        }

        @Bean
        InsertThenFailHandler insertThenFailHandler(JdbcTemplate jdbc) {
            return new InsertThenFailHandler(jdbc);
        }
    }

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.update("delete from shedlock where name = 'bus-test'");
    }

    @Test
    void dispatchesToTheMatchingHandler() {
        assertThat(bus.dispatch(new Echo("hi"))).isEqualTo("echo:hi");
    }

    @Test
    void runsHandlerInsideATransaction() {
        assertThat(bus.dispatch(new InTransaction())).isTrue();
    }

    @Test
    void rollsBackWhenHandlerThrows() {
        assertThatThrownBy(() -> bus.dispatch(new InsertThenFail())).isInstanceOf(IllegalStateException.class);

        Integer rows = jdbc.queryForObject("select count(*) from shedlock where name = 'bus-test'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    void rejectsInvalidCommandsBeforeCallingTheHandler() {
        assertThatThrownBy(() -> bus.dispatch(new Echo(" "))).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void askRunsQueriesInAReadOnlyTransaction() {
        assertThat(bus.ask(new ReadOnlyProbe())).isTrue();
    }
}
