package com.realestate.service;

import com.realestate.entity.Property;
import com.realestate.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Routing tests for the admin listings search ({@code getAllListings}):
 * a text query must hit the Specification path (searches every status),
 * while the no-query paths keep their original queries — the status path
 * deliberately sorts oldest-first because it doubles as the review queue.
 */
@ExtendWith(MockitoExtension.class)
class PropertyServiceAdminSearchTest {

    @Mock private PropertyRepository propertyRepository;

    @InjectMocks private PropertyService propertyService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    @SuppressWarnings("unchecked")
    void withKeyword_usesSpecificationQuery() {
        when(propertyRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        propertyService.getAllListings(Property.ListingStatus.ACTIVE, "peelamedu", pageable);

        verify(propertyRepository).findAll(any(Specification.class), eq(pageable));
        verify(propertyRepository, never()).findByStatusOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void withStatusAndNoKeyword_usesReviewQueueOrder() {
        when(propertyRepository.findByStatusOrderByCreatedAtAsc(Property.ListingStatus.PENDING_REVIEW, pageable))
            .thenReturn(Page.empty(pageable));

        propertyService.getAllListings(Property.ListingStatus.PENDING_REVIEW, null, pageable);

        verify(propertyRepository).findByStatusOrderByCreatedAtAsc(Property.ListingStatus.PENDING_REVIEW, pageable);
    }

    @Test
    void blankKeyword_isTreatedAsNoKeyword() {
        when(propertyRepository.findByStatusOrderByCreatedAtAsc(Property.ListingStatus.ACTIVE, pageable))
            .thenReturn(Page.empty(pageable));

        propertyService.getAllListings(Property.ListingStatus.ACTIVE, "   ", pageable);

        verify(propertyRepository).findByStatusOrderByCreatedAtAsc(Property.ListingStatus.ACTIVE, pageable);
    }

    @Test
    void noFilters_listsEverything() {
        when(propertyRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        propertyService.getAllListings(null, null, pageable);

        verify(propertyRepository).findAll(pageable);
    }
}
