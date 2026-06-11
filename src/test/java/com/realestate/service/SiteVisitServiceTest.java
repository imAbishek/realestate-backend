package com.realestate.service;

import com.realestate.dto.booking.BookingDtos.BookSiteVisitRequest;
import com.realestate.dto.booking.BookingDtos.CancelBookingRequest;
import com.realestate.entity.Property;
import com.realestate.entity.SiteVisitBooking;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.UnauthorizedException;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SiteVisitBookingRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Guard/error-path tests for {@link SiteVisitService} — the security-relevant branches
 * (booking availability, required contact, and who may cancel).
 */
@ExtendWith(MockitoExtension.class)
class SiteVisitServiceTest {

    @Mock private SiteVisitBookingRepository bookingRepo;
    @Mock private PropertyRepository         propertyRepo;
    @Mock private UserRepository             userRepo;

    @InjectMocks private SiteVisitService siteVisitService;

    private Property property(Property.ListingStatus status, String ownerEmail) {
        User owner = User.builder().id(UUID.randomUUID()).email(ownerEmail).build();
        return Property.builder().id(UUID.randomUUID()).status(status).owner(owner).build();
    }

    @Test
    void book_inactiveProperty_throws() {
        UUID id = UUID.randomUUID();
        when(propertyRepo.findById(id))
            .thenReturn(Optional.of(property(Property.ListingStatus.PENDING_REVIEW, "owner@x.in")));

        BookSiteVisitRequest req = new BookSiteVisitRequest();
        req.setContactName("Buyer");
        req.setContactPhone("9876543210");

        assertThatThrownBy(() -> siteVisitService.book(id, req, null))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void book_missingContactName_throws() {
        UUID id = UUID.randomUUID();
        when(propertyRepo.findById(id))
            .thenReturn(Optional.of(property(Property.ListingStatus.ACTIVE, "owner@x.in")));

        BookSiteVisitRequest req = new BookSiteVisitRequest(); // no contact name

        assertThatThrownBy(() -> siteVisitService.book(id, req, null))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cancel_byUnrelatedUser_throws() {
        Property p = property(Property.ListingStatus.ACTIVE, "owner@x.in");
        User booker = User.builder().id(UUID.randomUUID()).email("buyer@x.in").build();
        SiteVisitBooking booking = SiteVisitBooking.builder()
            .id(UUID.randomUUID()).property(p).user(booker)
            .status(SiteVisitBooking.Status.REQUESTED).build();
        when(bookingRepo.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
            siteVisitService.cancel(booking.getId(), new CancelBookingRequest(), "stranger@x.in"))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void cancel_alreadyCancelled_throws() {
        Property p = property(Property.ListingStatus.ACTIVE, "owner@x.in");
        SiteVisitBooking booking = SiteVisitBooking.builder()
            .id(UUID.randomUUID()).property(p).user(null)
            .status(SiteVisitBooking.Status.CANCELLED).build();
        when(bookingRepo.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Caller is the property owner (allowed to cancel) but it's already cancelled.
        assertThatThrownBy(() ->
            siteVisitService.cancel(booking.getId(), new CancelBookingRequest(), "owner@x.in"))
            .isInstanceOf(BadRequestException.class);
    }
}
