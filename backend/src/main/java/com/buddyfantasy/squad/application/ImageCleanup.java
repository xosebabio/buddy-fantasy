package com.buddyfantasy.squad.application;

import com.buddyfantasy.squad.application.port.out.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Best-effort deletion of images that are no longer referenced. It runs after the transaction commits, so a rollback
 * never leaves the database pointing at a deleted object; a failure only leaves an orphan behind, which is harmless.
 */
@Component
@RequiredArgsConstructor
public class ImageCleanup {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanup.class);

    private final ImageStorage images;

    public void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(key);
            }
        });
    }

    private void deleteQuietly(String key) {
        try {
            images.delete(key);
        } catch (RuntimeException e) {
            log.warn("Could not delete image {}: {}", key, e.toString());
        }
    }
}
