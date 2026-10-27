package com.buddyfantasy.squad.adapter.out.storage;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.buddyfantasy.squad.application.port.out.ImageUpload;
import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.application.port.out.StoredImage;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/** S3 adapter (AWS in production, SeaweedFS locally). Images are served by CloudFront, never from the bucket. */
class S3ImageStorage implements ImageStorage {

    /** Keys are immutable, so browsers and CloudFront may cache an image for a year. */
    static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
    /** Signed too, but set by the browser itself ({@code fetch} forbids them), so they are not handed to the client. */
    private static final Set<String> IMPLICIT_HEADERS = Set.of("host", "content-length");

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;
    private final Duration uploadTtl;
    private final Clock clock;

    S3ImageStorage(S3Client s3, S3Presigner presigner, String bucket, String publicBaseUrl, Duration uploadTtl,
                   Clock clock) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        this.uploadTtl = uploadTtl;
        this.clock = clock;
    }

    @Override
    public ImageUpload createUpload(String key, String contentType, long sizeBytes) {
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presign -> presign
                .signatureDuration(uploadTtl)
                .putObjectRequest(put -> put.bucket(bucket).key(key)
                        .contentType(contentType)
                        .contentLength(sizeBytes)
                        .cacheControl(CACHE_CONTROL)));
        Map<String, String> headers = presigned.signedHeaders().entrySet().stream()
                .filter(header -> !IMPLICIT_HEADERS.contains(header.getKey().toLowerCase()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, header -> String.join(",", header.getValue())));
        return new ImageUpload(presigned.url().toString(), headers, key, clock.instant().plus(uploadTtl));
    }

    @Override
    public Optional<StoredImage> find(String key) {
        try {
            HeadObjectResponse head = s3.headObject(request -> request.bucket(bucket).key(key));
            return Optional.of(new StoredImage(head.contentType(), head.contentLength()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(request -> request.bucket(bucket).key(key));
    }

    @Override
    public String publicUrl(String key) {
        return publicBaseUrl + "/" + key;
    }
}
