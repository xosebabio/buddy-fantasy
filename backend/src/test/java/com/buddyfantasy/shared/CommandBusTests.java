package com.buddyfantasy.shared;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.shared.application.Command;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.application.Query;
import com.buddyfantasy.shared.application.QueryHandler;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
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

    record EchoCommand(@NotBlank String text) implements Command<String> {
    }

    record InTransactionCommand() implements Command<Boolean> {
    }

    record ReadOnlyProbeQuery() implements Query<Boolean> {
    }

    record InsertThenFailCommand() implements Command<Void> {
    }

    static class EchoHandler implements CommandHandler<EchoCommand, String> {

        @Override
        public String handle(EchoCommand command) {
            return "echo:" + command.text();
        }
    }

    static class InTransactionHandler implements CommandHandler<InTransactionCommand, Boolean> {

        @Override
        public Boolean handle(InTransactionCommand command) {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }
    }

    static class ReadOnlyProbeHandler implements QueryHandler<ReadOnlyProbeQuery, Boolean> {

        @Override
        public Boolean handle(ReadOnlyProbeQuery query) {
            return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        }
    }

    static class InsertThenFailHandler implements CommandHandler<InsertThenFailCommand, Void> {

        private final JdbcTemplate jdbc;

        InsertThenFailHandler(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public Void handle(InsertThenFailCommand command) {
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

    @Test
    @DisplayName("Dispatches to the matching handler")
    void dispatchesToTheMatchingHandler() {
        assertThat(bus.dispatch(new EchoCommand("hi"))).isEqualTo("echo:hi");
    }

    @Test
    @DisplayName("Runs the handler inside a transaction")
    void runsHandlerInsideATransaction() {
        assertThat(bus.dispatch(new InTransactionCommand())).isTrue();
    }

    @Test
    @DisplayName("Rolls back when the handler throws")
    void rollsBackWhenHandlerThrows() {
        assertThatThrownBy(() -> bus.dispatch(new InsertThenFailCommand())).isInstanceOf(IllegalStateException.class);

        Integer rows = jdbc.queryForObject("select count(*) from shedlock where name = 'bus-test'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    @DisplayName("Rejects invalid commands before calling the handler")
    void rejectsInvalidCommandsBeforeCallingTheHandler() {
        assertThatThrownBy(() -> bus.dispatch(new EchoCommand(" "))).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("ask runs queries in a read-only transaction")
    void askRunsQueriesInAReadOnlyTransaction() {
        assertThat(bus.ask(new ReadOnlyProbeQuery())).isTrue();
    }
}
