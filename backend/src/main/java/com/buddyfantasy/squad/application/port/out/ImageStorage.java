package com.buddyfantasy.squad.application.port.out;

import java.util.Optional;

/**
 * Generic image storage: it knows nothing about players. The browser uploads directly with a presigned URL; the
 * backend never streams the image. Keys are chosen by the caller and are immutable: a new image gets a new key.
 */
public interface ImageStorage {

    /**
     * Presigned PUT that only accepts exactly {@code contentType} and {@code sizeBytes}: both are part of the
     * signature, so the upload fails if the client sends anything else.
     */
    ImageUpload createUpload(String key, String contentType, long sizeBytes);

    /** Metadata of the stored object, empty if nothing was uploaded under that key. */
    Optional<StoredImage> find(String key);

    /** Idempotent: deleting a missing key is not an error. */
    void delete(String key);
}
