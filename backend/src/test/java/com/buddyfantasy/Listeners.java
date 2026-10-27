package com.buddyfantasy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * The module events reach their listeners asynchronously after commit. Await them before checking their effects, so a
 * listener that throws fails the test instead of ending up only in the log.
 */
@UtilityClass
public class Listeners {

    private final Duration TIMEOUT = Duration.ofSeconds(10);
    private final Duration POLL_INTERVAL = Duration.ofMillis(10);

    /** Waits until every event publication is completed; fails listing the incomplete ones if they are not. */
    public void await(JdbcOperations jdbc) {
        try {
            Awaitility.await().atMost(TIMEOUT).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL).until(() -> jdbc.queryForObject(
                    "select count(*) from event_publication where completion_date is null", Integer.class) == 0);
        } catch (ConditionTimeoutException e) {
            List<Map<String, Object>> incomplete = jdbc.queryForList(
                    "select listener_id, event_type from event_publication where completion_date is null");
            throw new AssertionError("Event listeners did not complete within " + TIMEOUT + " (see the log): "
                    + incomplete, e);
        }
    }
}
