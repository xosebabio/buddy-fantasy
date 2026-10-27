package com.buddyfantasy.squad.adapter.out.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bucket of uploaded images (player photos for now). {@code endpoint} is only set for a local S3-compatible service (SeaweedFS), which is
 * then used with path-style URLs; in AWS it stays blank and the regional endpoint is used.
 * {@code publicBaseUrl} is where images are served from: CloudFront in production, the bucket itself locally.
 * {@code uploadTtl} is how long a presigned upload URL stays valid.
 * Credentials come from the SDK's default chain (IAM role in production, {@code AWS_*} variables locally).
 */
@ConfigurationProperties("squad.photos.s3")
record S3StorageProperties(String bucket, String region, String endpoint, String publicBaseUrl, Duration uploadTtl) {

    S3StorageProperties {
        if (uploadTtl == null || uploadTtl.isNegative() || uploadTtl.isZero()) {
            throw new IllegalArgumentException("squad.photos.s3.upload-ttl must be > 0");
        }
    }

    Optional<URI> endpointOverride() {
        return endpoint == null || endpoint.isBlank() ? Optional.empty() : Optional.of(URI.create(endpoint));
    }
}
