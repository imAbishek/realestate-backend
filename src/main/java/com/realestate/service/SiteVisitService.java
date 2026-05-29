package com.realestate.service;

import com.realestate.dto.booking.BookingDtos.*;
import com.realestate.entity.Property;
import com.realestate.entity.PropertyImage;
import com.realestate.entity.SiteVisitBooking;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.exception.UnauthorizedException;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SiteVisitBookingRepository;
import com.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteVisitService {

    private final SiteVisitBookingRepository bookingRepo;
    private final PropertyRepository         propertyRepo;
    private final UserRepository             userRepo;

    /**
     * Book a site visit. Both authenticated users and guests may book —
     * pass userEmail = null for guest bookings (then contact name/phone are required).
     */
    @Transactional
    public SiteVisitBookingResponse book(UUID propertyId, BookSiteVisitRequest req, String userEmail) {
        Property property = propertyRepo.findById(propertyId)
            .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));

        if (property.getStatus() != Property.ListingStatus.ACTIVE) {
            throw new BadRequestException("This listing is not available for booking right now");
        }

        if (req.getContactName() == null || req.getContactName().isBlank()) {
            throw new BadRequestException("Contact name is required");
        }
        if ((req.getContactPhone() == null || req.getContactPhone().isBlank())
                && (req.getContactEmail() == null || req.getContactEmail().isBlank())) {
            throw new BadRequestException("Provide at least a phone number or email so the owner can confirm");
        }

        User user = null;
        if (userEmail != null) {
            user = userRepo.findByEmail(userEmail).orElse(null);
        }

        SiteVisitBooking booking = SiteVisitBooking.builder()
            .property(property)
            .user(user)
            .contactName(req.getContactName().trim())
            .contactPhone(req.getContactPhone() != null ? req.getContactPhone().trim() : null)
            .contactEmail(req.getContactEmail() != null ? req.getContactEmail().trim() : null)
            .preferredDate(req.getPreferredDate())
            .preferredWindow(req.getPreferredWindow())
            .notes(req.getNotes())
            .status(SiteVisitBooking.Status.REQUESTED)
            .build();

        booking = bookingRepo.save(booking);
        log.info("Site visit booked: {} on property {} by {}",
                 booking.getId(), propertyId, userEmail != null ? userEmail : "guest");

        return SiteVisitBookingResponse.from(booking, primaryImageOf(property));
    }

    @Transactional(readOnly = true)
    public Page<SiteVisitBookingResponse> listMine(String userEmail, Pageable pageable) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("User not found"));

        return bookingRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
            .map(b -> SiteVisitBookingResponse.from(b, primaryImageOf(b.getProperty())));
    }

    @Transactional(readOnly = true)
    public Page<SiteVisitBookingResponse> listOnMyListings(String ownerEmail, Pageable pageable) {
        User owner = userRepo.findByEmail(ownerEmail)
            .orElseThrow(() -> new UnauthorizedException("User not found"));

        return bookingRepo.findByPropertyOwnerIdOrderByCreatedAtDesc(owner.getId(), pageable)
            .map(b -> SiteVisitBookingResponse.from(b, primaryImageOf(b.getProperty())));
    }

    @Transactional
    public SiteVisitBookingResponse cancel(UUID bookingId, CancelBookingRequest req, String userEmail) {
        SiteVisitBooking booking = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Only the buyer who made the booking OR the owner of the property can cancel.
        boolean isBuyer = booking.getUser() != null && booking.getUser().getEmail().equals(userEmail);
        boolean isOwner = booking.getProperty().getOwner().getEmail().equals(userEmail);
        if (!isBuyer && !isOwner) {
            throw new UnauthorizedException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == SiteVisitBooking.Status.CANCELLED
         || booking.getStatus() == SiteVisitBooking.Status.COMPLETED) {
            throw new BadRequestException("Booking is already " + booking.getStatus().name().toLowerCase());
        }

        booking.setStatus(SiteVisitBooking.Status.CANCELLED);
        booking.setCancelledBy(isBuyer
            ? SiteVisitBooking.CancelledBy.BUYER
            : SiteVisitBooking.CancelledBy.OWNER);
        if (req != null && req.getReason() != null) booking.setCancelReason(req.getReason());

        bookingRepo.save(booking);
        return SiteVisitBookingResponse.from(booking, primaryImageOf(booking.getProperty()));
    }

    private String primaryImageOf(Property p) {
        return p.getImages().stream()
            .filter(PropertyImage::isPrimary)
            .map(PropertyImage::getUrl)
            .findFirst()
            .orElseGet(() -> p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl());
    }
}
