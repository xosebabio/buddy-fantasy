package com.buddyfantasy.shared.infrastructure;

import java.util.List;
import java.util.Set;

import com.buddyfantasy.shared.application.Command;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.application.CommandHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dispatches commands to their handler. Pipeline: logging → bean validation → one transaction.
 * Per-league authorization will be added to this pipeline later.
 */
@Component
class SpringCommandBus implements CommandBus {

    private static final Logger log = LoggerFactory.getLogger(SpringCommandBus.class);

    private final HandlerRegistry<CommandHandler<?, ?>> registry;
    private final Validator validator;
    private final TransactionTemplate transaction;

    SpringCommandBus(List<CommandHandler<?, ?>> handlers, Validator validator, PlatformTransactionManager transactionManager) {
        this.registry = new HandlerRegistry<>(handlers, CommandHandler.class);
        this.validator = validator;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> R dispatch(Command<R> command) {
        CommandHandler handler = registry.find(command.getClass());
        String name = command.getClass().getSimpleName();
        long start = System.nanoTime();
        try {
            validate(command);
            return (R) transaction.execute(status -> handler.handle(command));
        } catch (RuntimeException e) {
            log.warn("Command {} failed: {}", name, e.toString());
            throw e;
        } finally {
            log.debug("Command {} took {} ms", name, (System.nanoTime() - start) / 1_000_000);
        }
    }

    private void validate(Command<?> command) {
        Set<ConstraintViolation<Command<?>>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
