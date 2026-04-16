package com.realestate.repository;

import com.realestate.dto.property.PropertySearchRequest;
import com.realestate.entity.Property;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic JPA query from a PropertySearchRequest.
 *
 * Every filter field is optional — only non-null values are added
 * to the WHERE clause. This is how Spring Data JPA handles
 * "search with optional filters" cleanly without messy if/else SQL.
 *
 * Usage:
 *   Specification<Property> spec = PropertySpecification.build(request);
 *   Page<Property> results = propertyRepository.findAll(spec, pageable);
 */
public class PropertySpecification {

    public static Specification<Property> build(PropertySearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter to ACTIVE listings for public search
            predicates.add(cb.equal(root.get("status"), Property.ListingStatus.ACTIVE));

            // ── Listing type (sale / rent / pg) ──────────
            if (req.getListingType() != null) {
                predicates.add(cb.equal(root.get("listingType"), req.getListingType()));
            }

            // ── Property type (apartment / villa / etc.) ─
            if (req.getPropertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), req.getPropertyType()));
            }

            // ── City (join through locality → city) ───────
            if (req.getCitySlug() != null && !req.getCitySlug().isBlank()) {
                Join<Object, Object> locality = root.join("locality", JoinType.INNER);
                Join<Object, Object> city = locality.join("city", JoinType.INNER);
                predicates.add(cb.equal(city.get("slug"), req.getCitySlug()));
            }

            // ── Locality ──────────────────────────────────
            if (req.getLocalityId() != null) {
                predicates.add(cb.equal(
                    root.get("locality").get("id"), req.getLocalityId()
                ));
            }

            // ── Price range ───────────────────────────────
            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), req.getMinPrice()));
            }
            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), req.getMaxPrice()));
            }

            // ── Bedrooms ──────────────────────────────────
            if (req.getMinBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                    root.get("bedrooms"), req.getMinBedrooms().shortValue()
                ));
            }
            if (req.getMaxBedrooms() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("bedrooms"), req.getMaxBedrooms().shortValue()
                ));
            }

            // ── Area ──────────────────────────────────────
            if (req.getMinArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("areaSqft"), req.getMinArea()));
            }
            if (req.getMaxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("areaSqft"), req.getMaxArea()));
            }

            // ── Furnishing ────────────────────────────────
            if (req.getFurnishing() != null) {
                predicates.add(cb.equal(root.get("furnishing"), req.getFurnishing()));
            }

            // ── Featured only ─────────────────────────────
            if (Boolean.TRUE.equals(req.getFeaturedOnly())) {
                predicates.add(cb.isTrue(root.get("isFeatured")));
            }

            // ── Keyword search (title, description, or city name) ─────
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String pattern = "%" + req.getKeyword().toLowerCase() + "%";
                Predicate titleMatch = cb.like(
                    cb.lower(root.get("title")), pattern
                );
                Predicate descMatch = cb.like(
                    cb.lower(root.get("description")), pattern
                );
                // Also match city name so typing "Coimbatore" in search finds city properties
                Join<Object, Object> kwLocality = root.join("locality", JoinType.LEFT);
                Join<Object, Object> kwCity = kwLocality.join("city", JoinType.LEFT);
                Predicate cityNameMatch = cb.like(cb.lower(kwCity.get("name")), pattern);
                predicates.add(cb.or(titleMatch, descMatch, cityNameMatch));
            }

            // Avoid N+1 on images fetch for listing cards
            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
