package com.buddyfantasy.squad;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.application.port.out.ImageUpload;
import com.buddyfantasy.squad.application.port.out.StoredImage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fake of {@link ImageStorage}: the real S3 adapter is covered by {@code S3ImageStorageTests}. */
public class InMemoryImageStorage implements ImageStorage {

    static final Instant EXPIRES_AT = Instant.parse("2026-01-10T10:05:00Z");

    private final Map<String, StoredImage> objects = new ConcurrentHashMap<>();
    private final List<String> deleted = new CopyOnWriteArrayList<>();
    private volatile boolean failDeletes;

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        @Primary
        InMemoryImageStorage inMemoryImageStorage() {
            return new InMemoryImageStorage();
        }
    }

    public void reset() {
        objects.clear();
        deleted.clear();
        failDeletes = false;
    }

    /** What the browser's PUT to the presigned URL would leave in the bucket. */
    public void upload(String key, String contentType, long sizeBytes) {
        objects.put(key, new StoredImage(contentType, sizeBytes));
    }

    public List<String> deleted() {
        return List.copyOf(deleted);
    }

    public void failDeletes() {
        failDeletes = true;
    }

    @Override
    public ImageUpload createUpload(String key, String contentType, long sizeBytes) {
        return new ImageUpload("https://upload.test/" + key, Map.of("content-type", contentType), key, EXPIRES_AT);
    }

    @Override
    public Optional<StoredImage> find(String key) {
        return Optional.ofNullable(objects.get(key));
    }

    @Override
    public void delete(String key) {
        if (failDeletes) {
            throw new IllegalStateException("storage unavailable");
        }
        objects.remove(key);
        deleted.add(key);
    }

    @Override
    public String publicUrl(String key) {
        return "https://cdn.test/" + key;
    }
}
