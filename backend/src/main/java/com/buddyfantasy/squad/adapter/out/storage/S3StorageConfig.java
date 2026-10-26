package com.buddyfantasy.squad.adapter.out.storage;

import java.time.Clock;

import com.buddyfantasy.squad.application.port.out.PlayerPhotoStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3PhotoProperties.class)
class S3StorageConfig {

    @Bean(destroyMethod = "close")
    S3Client photoS3Client(S3PhotoProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                // only checksum when S3 requires it: keeps requests compatible with S3-compatible services
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
        properties.endpointOverride().ifPresent(endpoint -> builder.endpointOverride(endpoint).forcePathStyle(true));
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner photoS3Presigner(S3PhotoProperties properties) {
        S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(properties.region()));
        properties.endpointOverride().ifPresent(endpoint -> builder.endpointOverride(endpoint)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()));
        return builder.build();
    }

    @Bean
    PlayerPhotoStorage playerPhotoStorage(S3Client photoS3Client, S3Presigner photoS3Presigner,
                                          S3PhotoProperties properties, Clock clock) {
        return new S3PlayerPhotoStorage(photoS3Client, photoS3Presigner, properties.bucket(), clock);
    }
}
