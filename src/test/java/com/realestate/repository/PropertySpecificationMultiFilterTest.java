package com.realestate.repository;

import com.realestate.dto.property.PropertySearchRequest;
import com.realestate.entity.Property;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The multi-select filters (item 8) must translate into IN predicates so a search
 * for several property types / listing types / furnishings returns their union.
 * We verify {@code path.in(...)} is invoked for each populated list, and skipped
 * when the list is null/empty (so single-value links keep working).
 */
class PropertySpecificationMultiFilterTest {

    @SuppressWarnings("unchecked")
    private void runBuild(PropertySearchRequest req,
                          Path<Object> listingTypePath,
                          Path<Object> propertyTypePath,
                          Path<Object> furnishingPath) {
        Root<Property> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate pred = mock(Predicate.class);

        when(root.get("status")).thenReturn((Path) mock(Path.class));
        when(root.get("listingType")).thenReturn(listingTypePath);
        when(root.get("propertyType")).thenReturn(propertyTypePath);
        when(root.get("furnishing")).thenReturn(furnishingPath);
        when(cb.equal(any(), any())).thenReturn(pred);
        when(cb.and(any(Predicate[].class))).thenReturn(pred);
        when(listingTypePath.in(anyCollection())).thenReturn(pred);
        when(propertyTypePath.in(anyCollection())).thenReturn(pred);
        when(furnishingPath.in(anyCollection())).thenReturn(pred);

        PropertySpecification.build(req).toPredicate(root, query, cb);
    }

    @Test
    @SuppressWarnings("unchecked")
    void populatedLists_emitInPredicates() {
        Path<Object> listingTypePath  = mock(Path.class);
        Path<Object> propertyTypePath = mock(Path.class);
        Path<Object> furnishingPath   = mock(Path.class);

        PropertySearchRequest req = new PropertySearchRequest();
        req.setListingTypes(List.of(Property.ListingType.SALE, Property.ListingType.RENT));
        req.setPropertyTypes(List.of(Property.PropertyType.APARTMENT, Property.PropertyType.VILLA));
        req.setFurnishings(List.of(Property.FurnishingStatus.SEMI_FURNISHED));

        runBuild(req, listingTypePath, propertyTypePath, furnishingPath);

        verify(listingTypePath).in(List.of(Property.ListingType.SALE, Property.ListingType.RENT));
        verify(propertyTypePath).in(List.of(Property.PropertyType.APARTMENT, Property.PropertyType.VILLA));
        verify(furnishingPath).in(List.of(Property.FurnishingStatus.SEMI_FURNISHED));
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyOrNullLists_emitNoInPredicates() {
        Path<Object> listingTypePath  = mock(Path.class);
        Path<Object> propertyTypePath = mock(Path.class);
        Path<Object> furnishingPath   = mock(Path.class);

        PropertySearchRequest req = new PropertySearchRequest();
        req.setListingTypes(List.of());     // empty → no predicate
        req.setPropertyTypes(null);         // null  → no predicate
        // furnishings left null

        runBuild(req, listingTypePath, propertyTypePath, furnishingPath);

        verify(listingTypePath, never()).in(anyCollection());
        verify(propertyTypePath, never()).in(anyCollection());
        verify(furnishingPath, never()).in(anyCollection());
    }
}
