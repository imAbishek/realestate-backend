package com.realestate.dto.property;

import com.realestate.entity.Property;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents all the optional query parameters for property search.
 *
 * Example URL:
 * GET /api/search/properties?citySlug=coimbatore&listingType=RENT
 *   &propertyType=APARTMENT&minPrice=10000&maxPrice=30000
 *   &minBedrooms=2&furnishing=SEMI_FURNISHED&page=0&size=20&sort=price,asc
 *
 * Spring automatically maps request params to this class fields.
 */
@Data
public class PropertySearchRequest {

    // Location
    private String citySlug;
    private UUID   localityId;

    // Type filters
    private Property.ListingType    listingType;
    private Property.PropertyType   propertyType;

    // Price
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Size
    private Integer minBedrooms;
    private Integer maxBedrooms;
    private BigDecimal minArea;
    private BigDecimal maxArea;

    // Other filters
    private Property.FurnishingStatus furnishing;
    private Boolean featuredOnly;

    // Keyword
    private String keyword;
}
