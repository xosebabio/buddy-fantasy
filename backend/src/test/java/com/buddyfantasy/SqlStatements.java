package com.buddyfantasy;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Counts the SQL statements a piece of code runs on the test's thread (RNF-09): every execution sent to the database,
 * through JPA, {@code JdbcClient} or {@code JdbcTemplate} alike, a batch counting once. Every Spring test's
 * {@code DataSource} is wrapped by {@link #proxyDataSources()} (registered by {@link PostgresTestConfig}); the
 * statements of other threads, such as async listeners, are not counted.
 */
public final class SqlStatements {

    private static final ThreadLocal<List<String>> RECORDING = new ThreadLocal<>();

    private SqlStatements() {
    }

    /** Code that may throw, such as a MockMvc request. */
    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    /** The statements {@code action} ran on this thread, in order, one per execution. */
    public static List<String> during(Action action) throws Exception {
        List<String> statements = new ArrayList<>();
        RECORDING.set(statements);
        try {
            action.run();
        } finally {
            RECORDING.remove();
        }
        return List.copyOf(statements);
    }

    /** Wraps every {@code DataSource} bean so that {@link #during} sees its statements. */
    static BeanPostProcessor proxyDataSources() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder.create(dataSource).name(beanName).listener(new Recorder()).build();
                }
                return bean;
            }
        };
    }

    private static final class Recorder implements QueryExecutionListener {

        @Override
        public void beforeQuery(ExecutionInfo execution, List<QueryInfo> queries) {
        }

        @Override
        public void afterQuery(ExecutionInfo execution, List<QueryInfo> queries) {
            List<String> statements = RECORDING.get();
            if (statements != null) {
                statements.add(String.join("; ", queries.stream().map(QueryInfo::getQuery).toList()));
            }
        }
    }
}
