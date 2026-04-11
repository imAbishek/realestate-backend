package com.realestate.dto.property;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.entity.Property;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PropertyDtos {

    // ─────────────────────────────────────────────
    // REQUEST: Create / Update a property
    // ─────────────────────────────────────────────

    @Data
    public static class PropertyRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title is too long")
        private String title;

        @Size(max = 5000, message = "Description too long")
        private String description;

        @NotNull(message = "Listing type is required (SALE, RENT, PG)")
        private Property.ListingType listingType;

        @NotNull(message = "Property type is required")
        private Property.PropertyType propertyType;

        @NotNull(message = "Locality ID is required")
        private UUID localityId;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private BigDecimal price;

        private Property.PriceUnit priceUnit;
        private boolean priceNegotiable;
        private BigDecimal securityDeposit;

        @Min(value = 0, message = "Bedrooms cannot be negative")
        @Max(value = 20, message = "Bedrooms value seems too high")
        private Integer bedrooms;

        private Integer bathrooms;
        private Integer balconies;
        private Integer totalFloors;
        private Integer floorNumber;

        @NotNull(message = "Area is required")
        @DecimalMin(value = "1.0", message = "Area must be at least 1 sqft")
        private BigDecimal areaSqft;

        private BigDecimal carpetAreaSqft;
        private Property.FurnishingStatus furnishing;
        private String facing;
        private Integer ageOfProperty;
        private LocalDate availableFrom;
        private boolean parkingAvailable;

        private String addressLine;
        private BigDecimal latitude;
        private BigDecimal longitude;

        // IDs of amenities from the amenities master table
        private List<UUID> amenityIds;
    }

    // ─────────────────────────────────────────────
    // RESPONSE: Property card (list view)
    // Lightweight — no description, fewer fields
    // ─────────────────────────────────────────────

    @Data @Builder
    @JsonIgnoreProperties({"featured", "verified"})
    public static class PropertyCardResponse {
        private UUID   id;
        private String title;
        private String listingType;
        private String propertyType;
        private String status;
        private BigDecimal price;
        private String priceUnit;
        private Integer bedrooms;
        private Integer bathrooms;
        private BigDecimal areaSqft;
        private String furnishing;
        private String localityName;
        private String cityName;
        @JsonProperty("isFeatured")
        private boolean isFeatured;
        @JsonProperty("isVerified")
        private boolean isVerified;
        private String primaryImageUrl;
        private int viewsCount;
        private LocalDateTime createdAt;
    }

    // ─────────────────────────────────────────────
    // RESPONSE: Full property detail page
    // ─────────────────────────────────────────────

    @Data @Builder
    @JsonIgnoreProperties({"featured", "verified"})
    public static class PropertyDetailResponse {
        private UUID   id;
        private String title;
        private String description;
        private String listingType;
        private String propertyType;
        private String status;
        private String rejectionReason;

        private BigDecimal price;
        private String priceUnit;
        private boolean priceNegotiable;
        private BigDecimal securityDeposit;

        private Integer bedrooms;
        private Integer bathrooms;
        private Integer balconies;
        private Integer totalFloors;
        private Integer floorNumber;
        private BigDecimal areaSqft;
        private BigDecimal carpetAreaSqft;

        private String furnishing;
        private String facing;
        private Integer ageOfProperty;
        private LocalDate availableFrom;
        private boolean parkingAvailable;

        private String addressLine;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String localityName;
        private String localitySlug;
        private String cityName;
        private String citySlug;

        @JsonProperty("isFeatured")
        private boolean isFeatured;
        @JsonProperty("isVerified")
        private boolean isVerified;
        private int viewsCount;
        private int inquiryCount;

        private List<ImageResponse> images;
        private List<AmenityResponse> amenities;
        private OwnerInfo owner;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
    }

    // ─────────────────────────────────────────────
    // Nested response types
    // ─────────────────────────────────────────────

    @Data @Builder
    @JsonIgnoreProperties("primary")
    public static class ImageResponse {
        private UUID id;
        private String url;
        @JsonProperty("isPrimary")
        private boolean isPrimary;
        private int sortOrder;
    }

    @Data @Builder
    public static class AmenityResponse {
        private UUID id;
        private String name;
        private String category;
        private String iconKey;
    }

    @Data @Builder
    public static class OwnerInfo {
        private UUID id;
        private String name;
        private String phone;
        private String profilePhotoUrl;
        private String role;
        // Agent-specific fields (null for regular sellers)
        private String agencyName;
        private Double avgRating;
    }

    // ─────────────────────────────────────────────
    // Admin: status update
    // ─────────────────────────────────────────────

    @Data
    public static class StatusUpdateRequest {
        private Property.ListingStatus status;
        private String rejectionReason;  // required only when status = REJECTED
    }
}
