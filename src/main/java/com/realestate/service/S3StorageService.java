package com.realestate.service;

import com.realestate.config.AppProperties;
import com.realestate.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AWS S3 storage implementation — active in prod profile only.
 *
 * Images are stored at key: properties/{propertyId}/{uuid}.{ext}
 * Returned URL: https://{bucket}.s3.{region}.amazonaws.com/properties/{propertyId}/{uuid}.{ext}
 *
 * Bucket must have public read enabled (see AWS setup guide in CLAUDE.md).
 */
@Service
@Profile("prod")
@Slf4j
public class S3StorageService implements StorageService {

    private static final long         MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final List<String> ALLOWED_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final S3Client s3Client;
    private final String   bucket;
    private final String   imageBaseUrl;   // root of public image URLs

    public S3StorageService(S3Client s3Client, AppProperties appProperties) {
        this.s3Client = s3Client;
        AppProperties.Aws aws = appProperties.getAws();
        this.bucket = aws.getS3().getBucketName();

        String endpoint  = aws.getEndpoint();
        String publicUrl = aws.getPublicUrl();

        if (publicUrl != null && !publicUrl.isBlank()) {
            // R2 pub URL: bucket is implicit — path is just the key, no bucket prefix
            // e.g. https://pub-xxx.r2.dev/properties/{id}/{file}  (NOT /bucket/properties/...)
            this.imageBaseUrl = publicUrl.stripTrailing();
            log.info("S3StorageService active (R2/CDN) — api: {}, public: {}, bucket: {}", endpoint, publicUrl, bucket);
        } else if (endpoint != null && !endpoint.isBlank()) {
            // MinIO self-hosted — same host serves both API and public reads
            this.imageBaseUrl = endpoint + "/" + bucket;
            log.info("S3StorageService active (MinIO) — endpoint: {}, bucket: {}", endpoint, bucket);
        } else {
            // AWS S3 virtual-hosted URL
            this.imageBaseUrl = "https://%s.s3.%s.amazonaws.com".formatted(bucket, aws.getRegion());
            log.info("S3StorageService active (AWS S3) — bucket: {}, region: {}", bucket, aws.getRegion());
        }
    }

    @Override
    public String uploadPropertyImage(MultipartFile file, UUID propertyId) {
        validateFile(file);

        String ext      = getExtension(file.getContentType());
        String key      = "properties/%s/%s.%s".formatted(propertyId, UUID.randomUUID(), ext);

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = imageBaseUrl + "/" + key;
            log.info("Image uploaded: {}", url);
            return url;

        } catch (IOException e) {
            log.error("S3 upload failed for property {}: {}", propertyId, e.getMessage());
            throw new RuntimeException("Image upload failed. Please try again.", e);
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        if (key == null) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("S3 image deleted: {}", key);
        } catch (Exception e) {
            log.warn("Could not delete S3 object for URL: {}", imageUrl);
        }
    }

    @Override
    public void deleteAllPropertyImages(UUID propertyId) {
        String prefix = "properties/%s/".formatted(propertyId);
        try {
            var listReq = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
            var objects = s3Client.listObjectsV2(listReq).contents();
            if (objects.isEmpty()) return;

            List<ObjectIdentifier> toDelete = objects.stream()
                .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                .collect(Collectors.toList());

            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(d -> d.objects(toDelete))
                .build());

            log.info("Deleted {} S3 images for property {}", toDelete.size(), propertyId);
        } catch (Exception e) {
            log.warn("Could not delete S3 images for property {}: {}", propertyId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("No file provided");
        if (file.getSize() > MAX_FILE_SIZE)
            throw new BadRequestException("File size exceeds 10 MB limit");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new BadRequestException("Invalid file type. Only JPEG, PNG and WebP are allowed.");
    }

    private String getExtension(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png"               -> "png";
            case "image/webp"              -> "webp";
            default                        -> "jpg";
        };
    }

    /**
     * Extract the S3 object key from a public URL.
     * AWS format:   https://{bucket}.s3.{region}.amazonaws.com/{key}
     * MinIO format: http://{host}:{port}/{bucket}/{key}
     */
    private String extractKeyFromUrl(String url) {
        if (url == null) return null;
        // AWS virtual-hosted URL
        int idx = url.indexOf(".amazonaws.com/");
        if (idx >= 0) return url.substring(idx + ".amazonaws.com/".length());
        // MinIO path-style URL: strip base prefix "{endpoint}/{bucket}/"
        String prefix = imageBaseUrl + "/";
        if (url.startsWith(prefix)) return url.substring(prefix.length());
        return null;
    }
}
