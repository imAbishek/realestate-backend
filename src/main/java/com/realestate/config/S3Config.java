package com.realestate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Creates the S3Client bean — active in prod Spring profile.
 * Supports both real AWS S3 and MinIO (S3-compatible).
 *
 * For MinIO: set MINIO_ENDPOINT=http://your-server:9000 in env.
 * For AWS:   leave MINIO_ENDPOINT blank; set AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_REGION, S3_BUCKET.
 */
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class S3Config {

    private final AppProperties appProperties;

    @Bean
    public S3Client s3Client() {
        AppProperties.Aws aws = appProperties.getAws();

        var builder = S3Client.builder()
            .region(Region.of(aws.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(aws.getAccessKey(), aws.getSecretKey())
            ));

        // MinIO requires a custom endpoint + path-style URLs.
        // AWS S3 uses virtual-hosted style (no override needed).
        if (aws.getEndpoint() != null && !aws.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(aws.getEndpoint()))
                   .serviceConfiguration(S3Configuration.builder()
                       .pathStyleAccessEnabled(true)
                       .build());
        }

        return builder.build();
    }
}
