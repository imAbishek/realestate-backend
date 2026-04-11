package com.realestate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "properties")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ── Ownership ─────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locality_id", nullable = false)
    private Locality locality;

    // ── Listing type ──────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    // ── Content ───────────────────────────────────
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Price ─────────────────────────────────────
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", length = 15)
    @Builder.Default
    private PriceUnit priceUnit = PriceUnit.TOTAL;

    @Column(name = "price_negotiable")
    @Builder.Default
    private boolean priceNegotiable = false;

    @Column(name = "security_deposit", precision = 14, scale = 2)
    private BigDecimal securityDeposit;

    // ── Size ──────────────────────────────────────
    private Short bedrooms;
    private Short bathrooms;
    private Short balconies;

    @Column(name = "total_floors")
    private Short totalFloors;

    @Column(name = "floor_number")
    private Short floorNumber;

    @Column(name = "area_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaSqft;

    @Column(name = "carpet_area_sqft", precision = 10, scale = 2)
    private BigDecimal carpetAreaSqft;

    // ── Details ───────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing", length = 20)
    @Builder.Default
    private FurnishingStatus furnishing = FurnishingStatus.UNFURNISHED;

    @Column(length = 20)
    private String facing;

    @Column(name = "age_of_property")
    private Short ageOfProperty;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "parking_available")
    @Builder.Default
    private boolean parkingAvailable = false;

    // ── Location ──────────────────────────────────
    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    // ── Platform flags ────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.PENDING_REVIEW;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean isFeatured = false;

    @Column(name = "is_verified")
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "views_count")
    @Builder.Default
    private int viewsCount = 0;

    @Column(name = "inquiry_count")
    @Builder.Default
    private int inquiryCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Relationships ─────────────────────────────
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PropertyImage> images = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "property_amenities",
        joinColumns = @JoinColumn(name = "property_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    // ─────────────────────────────────────────────
    // Enums
    // ─────────────────────────────────────────────

    public enum ListingType  { SALE, RENT, PG }

    public enum PropertyType {
        APARTMENT, INDEPENDENT_HOUSE, VILLA,
        PLOT, COMMERCIAL_OFFICE, COMMERCIAL_SHOP,
        BUILDER_FLOOR, PG_HOSTEL
    }

    public enum FurnishingStatus { UNFURNISHED, SEMI_FURNISHED, FULLY_FURNISHED }

    public enum PriceUnit { TOTAL, PER_MONTH, PER_SQFT }

    public enum ListingStatus {
        DRAFT, PENDING_REVIEW, ACTIVE, EXPIRED, REJECTED, SOLD_RENTED
    }
}
