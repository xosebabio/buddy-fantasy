package com.buddyfantasy.squad.adapter.out.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.squad.application.port.out.ImageUpload;
import com.buddyfantasy.squad.application.port.out.StoredImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Runs the real adapter and its Spring config against SeaweedFS, which verifies SigV4 signatures like S3. */
@Testcontainers
class S3ImageStorageTests {

    static final String ACCESS_KEY = "test-access";
    static final String SECRET_KEY = "test-secret";
    static final String BUCKET = "photos";
    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Container
    static final GenericContainer<?> SEAWEEDFS = new GenericContainer<>("chrislusf/seaweedfs:4.48")
            .withCopyToContainer(Transferable.of("""
                    {"identities": [{"name": "test",
                      "credentials": [{"accessKey": "%s", "secretKey": "%s"}],
                      "actions": ["Admin", "Read", "Write", "List"]}]}""".formatted(ACCESS_KEY, SECRET_KEY)),
                    "/etc/seaweedfs/s3.json")
            .withCommand("server", "-dir=/data", "-s3", "-s3.port=8333", "-s3.config=/etc/seaweedfs/s3.json",
                    "-master.volumeSizeLimitMB=64")
            .withExposedPorts(8333)
            .waitingFor(Wait.forHttp("/healthz").forPort(8333));

    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final Duration UPLOAD_TTL = Duration.ofMinutes(3);

    static S3Client s3;
    static S3Presigner presigner;
    static S3ImageStorage storage;

    @BeforeAll
    static void setUp() {
        // the config uses the SDK's default credentials chain, which reads these system properties
        System.setProperty("aws.accessKeyId", ACCESS_KEY);
        System.setProperty("aws.secretAccessKey", SECRET_KEY);
        S3StorageProperties properties = new S3StorageProperties(BUCKET, "eu-west-1",
                "http://" + SEAWEEDFS.getHost() + ":" + SEAWEEDFS.getMappedPort(8333), "https://cdn.test/", UPLOAD_TTL);
        S3StorageConfig config = new S3StorageConfig();
        s3 = config.imageS3Client(properties);
        presigner = config.imageS3Presigner(properties);
        storage = new S3ImageStorage(s3, presigner, BUCKET, properties.publicBaseUrl(), properties.uploadTtl(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        s3.createBucket(request -> request.bucket(BUCKET));
    }

    @AfterAll
    static void tearDown() {
        s3.close();
        presigner.close();
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
    }

    static String newKey() {
        return "leagues/" + UUID.randomUUID() + "/players/" + UUID.randomUUID() + ".jpg";
    }

    static int put(ImageUpload upload, Map<String, String> headers, byte[] body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(upload.uploadUrl()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach(request::header);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void presignedUploadAcceptsExactlyTheDeclaredFile() throws Exception {
        String key = newKey();
        ImageUpload upload = storage.createUpload(key, "image/jpeg", 1024);

        assertThat(upload.key()).isEqualTo(key);
        assertThat(upload.expiresAt()).isEqualTo(NOW.plus(UPLOAD_TTL));
        // the SDK lowercases signed header names; HTTP header names are case-insensitive
        assertThat(upload.headers()).containsOnly(
                Map.entry("content-type", "image/jpeg"),
                Map.entry("cache-control", S3ImageStorage.CACHE_CONTROL));

        assertThat(put(upload, upload.headers(), new byte[1024])).isEqualTo(200);

        assertThat(storage.find(key)).contains(new StoredImage("image/jpeg", 1024));
        assertThat(s3.headObject(request -> request.bucket(BUCKET).key(key)).cacheControl())
                .isEqualTo(S3ImageStorage.CACHE_CONTROL);
    }

    @Test
    void presignedUploadRejectsAnotherContentType() throws Exception {
        String key = newKey();
        ImageUpload upload = storage.createUpload(key, "image/jpeg", 1024);

        int status = put(upload, Map.of("Content-Type", "image/png",
                "Cache-Control", S3ImageStorage.CACHE_CONTROL), new byte[1024]);

        assertThat(status).isEqualTo(403);
        assertThat(storage.find(key)).isEmpty();
    }

    @Test
    void presignedUploadRejectsAnotherSize() throws Exception {
        String key = newKey();
        ImageUpload upload = storage.createUpload(key, "image/jpeg", 1024);

        assertThat(put(upload, upload.headers(), new byte[2048])).isEqualTo(403);
        assertThat(put(upload, upload.headers(), new byte[10])).isEqualTo(403);
        assertThat(storage.find(key)).isEmpty();
    }

    @Test
    void presignedUploadRejectsAMissingCacheControl() throws Exception {
        String key = newKey();
        ImageUpload upload = storage.createUpload(key, "image/jpeg", 1024);

        assertThat(put(upload, Map.of("Content-Type", "image/jpeg"), new byte[1024])).isEqualTo(403);
        assertThat(storage.find(key)).isEmpty();
    }

    @Test
    void publicUrlIsTheBaseUrlPlusTheKey() {
        // the trailing slash of the configured base URL is not duplicated
        assertThat(storage.publicUrl("leagues/a/players/b/c.webp"))
                .isEqualTo("https://cdn.test/leagues/a/players/b/c.webp");
    }

    @Test
    void findIsEmptyForAMissingKey() {
        assertThat(storage.find(newKey())).isEmpty();
    }

    @Test
    void deleteRemovesTheObjectAndIsIdempotent() throws Exception {
        String key = newKey();
        ImageUpload upload = storage.createUpload(key, "image/webp", 16);
        assertThat(put(upload, upload.headers(), new byte[16])).isEqualTo(200);

        storage.delete(key);

        assertThat(storage.find(key)).isEmpty();
        assertThatCode(() -> storage.delete(key)).doesNotThrowAnyException();
        assertThatCode(() -> storage.delete(newKey())).doesNotThrowAnyException();
    }
}
