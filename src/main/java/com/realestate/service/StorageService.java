package com.realestate.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Abstraction over image storage backends.
 * Dev profile: LocalStorageService (saves to disk, served via /uploads/**)
 * Prod profile: S3StorageService (uploads to AWS S3, served via CloudFront or S3 URLs)
 */
public interface StorageService {

    /**
     * Upload a property image and return its publicly accessible URL.
     */
    String uploadPropertyImage(MultipartFile file, UUID propertyId);

    /**
     * Delete a single image by its URL.
     */
    void deleteImage(String imageUrl);

    /**
     * Delete all images for a property (called on property deletion).
     */
    void deleteAllPropertyImages(UUID propertyId);
}
