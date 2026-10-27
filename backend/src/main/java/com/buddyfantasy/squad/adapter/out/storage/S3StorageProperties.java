package com.buddyfantasy.squad.adapter.out.storage;

import java.net.URI;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bucket of uploaded images (player photos for now). {@code endpoint} is only set for a local S3-compatible service (SeaweedFS), which is
 * then used with path-style URLs; in AWS it stays blank and the regional endpoint is used.
 * {@code publicBaseUrl} is where images are served from: CloudFront in production, the bucket itself locally.
 * Credentials come from the SDK's default chain (IAM role in production, {@code AWS_*} variables locally).
 */
@ConfigurationProperties("squad.photos.s3")
record S3StorageProperties(String bucket, String region, String endpoint, String publicBaseUrl) {

    Optional<URI> endpointOverride() {
        return endpoint == null || endpoint.isBlank() ? Optional.empty() : Optional.of(URI.create(endpoint));
    }
}
