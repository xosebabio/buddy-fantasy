package com.buddyfantasy.shared.infrastructure;

import java.util.List;

import com.buddyfantasy.shared.application.Query;
import com.buddyfantasy.shared.application.QueryBus;
import com.buddyfantasy.shared.application.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Dispatches queries inside a read-only transaction. */
@Component
class SpringQueryBus implements QueryBus {

    private final HandlerRegistry<QueryHandler<?, ?>> registry;
    private final TransactionTemplate readOnly;

    SpringQueryBus(List<QueryHandler<?, ?>> handlers, PlatformTransactionManager transactionManager) {
        this.registry = new HandlerRegistry<>(handlers, QueryHandler.class);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> R dispatch(Query<R> query) {
        QueryHandler handler = registry.find(query.getClass());
        return (R) readOnly.execute(status -> handler.handle(query));
    }
}
