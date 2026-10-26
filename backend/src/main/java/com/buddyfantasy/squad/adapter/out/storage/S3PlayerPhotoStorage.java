package com.buddyfantasy.squad.adapter.out.storage;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.buddyfantasy.squad.application.port.out.PhotoUpload;
import com.buddyfantasy.squad.application.port.out.PlayerPhotoStorage;
import com.buddyfantasy.squad.application.port.out.StoredPhoto;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/** S3 adapter (AWS in production, SeaweedFS locally). Photos are served by CloudFront, never from the bucket. */
class S3PlayerPhotoStorage implements PlayerPhotoStorage {

    static final Duration UPLOAD_TTL = Duration.ofMinutes(5);
    /** Keys are immutable, so browsers and CloudFront may cache a photo for a year. */
    static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
    /** Signed too, but set by the browser itself ({@code fetch} forbids them), so they are not handed to the client. */
    private static final Set<String> IMPLICIT_HEADERS = Set.of("host", "content-length");

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Clock clock;

    S3PlayerPhotoStorage(S3Client s3, S3Presigner presigner, String bucket, Clock clock) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.clock = clock;
    }

    @Override
    public PhotoUpload createUpload(String key, String contentType, long sizeBytes) {
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presign -> presign
                .signatureDuration(UPLOAD_TTL)
                .putObjectRequest(put -> put.bucket(bucket).key(key)
                        .contentType(contentType)
                        .contentLength(sizeBytes)
                        .cacheControl(CACHE_CONTROL)));
        Map<String, String> headers = presigned.signedHeaders().entrySet().stream()
                .filter(header -> !IMPLICIT_HEADERS.contains(header.getKey().toLowerCase()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, header -> String.join(",", header.getValue())));
        return new PhotoUpload(presigned.url().toString(), headers, key, clock.instant().plus(UPLOAD_TTL));
    }

    @Override
    public Optional<StoredPhoto> find(String key) {
        try {
            HeadObjectResponse head = s3.headObject(request -> request.bucket(bucket).key(key));
            return Optional.of(new StoredPhoto(head.contentType(), head.contentLength()));
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
}
