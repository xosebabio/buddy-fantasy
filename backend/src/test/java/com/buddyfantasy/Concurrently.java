package com.buddyfantasy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.experimental.UtilityClass;

/**
 * Runs tasks at the same time, to reproduce races against the real database: each on its own thread, all released
 * together by one latch once every thread is ready. Check the final state, never timings.
 */
@UtilityClass
public class Concurrently {

    private final long TIMEOUT_SECONDS = 30;

    /** Runs {@code task} on {@code threads} threads at once and returns its results. */
    public <T> List<T> run(int threads, Callable<T> task) {
        List<Callable<T>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(task);
        }
        return run(tasks);
    }

    /** Runs every task at once and returns their results, in the order of {@code tasks}. */
    public <T> List<T> run(List<Callable<T>> tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } catch (ExecutionException e) {
            throw new AssertionError("A concurrent task failed", e.getCause());
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError("The concurrent tasks did not finish", e);
        } finally {
            executor.shutdownNow();
        }
    }
}
