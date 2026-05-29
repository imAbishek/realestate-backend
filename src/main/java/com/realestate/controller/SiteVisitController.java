package com.realestate.controller;

import com.realestate.dto.booking.BookingDtos.*;
import com.realestate.service.SiteVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Site visit booking endpoints.
 *
 * Public:
 *   POST /properties/{id}/site-visits  — guests may book without an account
 *
 * Authenticated:
 *   GET    /site-visits               — my bookings as a buyer
 *   GET    /site-visits/incoming       — bookings on properties I own
 *   PATCH  /site-visits/{id}/cancel    — cancel my own (or my own listing's)
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Site visits", description = "Book and manage property site visits")
public class SiteVisitController {

    private final SiteVisitService bookingService;

    // ── Public (guest-friendly) booking ───────────────────────

    @PostMapping("/properties/{propertyId}/site-visits")
    @Operation(summary = "Book a site visit (logged-in or guest)")
    public ResponseEntity<SiteVisitBookingResponse> book(
            @PathVariable UUID propertyId,
            @Valid @RequestBody BookSiteVisitRequest req,
            @AuthenticationPrincipal UserDetails currentUser) {
        String email = currentUser != null ? currentUser.getUsername() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookingService.book(propertyId, req, email));
    }

    // ── Authenticated ─────────────────────────────────────────

    @GetMapping("/site-visits")
    @Operation(summary = "List bookings the current user has made",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<SiteVisitBookingResponse>> listMine(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails currentUser) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(bookingService.listMine(currentUser.getUsername(), pageable));
    }

    @GetMapping("/site-visits/incoming")
    @Operation(summary = "List bookings on listings owned by the current user",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<SiteVisitBookingResponse>> listIncoming(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails currentUser) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(bookingService.listOnMyListings(currentUser.getUsername(), pageable));
    }

    @PatchMapping("/site-visits/{id}/cancel")
    @Operation(summary = "Cancel a site visit booking (buyer or owner only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SiteVisitBookingResponse> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelBookingRequest req,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(bookingService.cancel(id, req, currentUser.getUsername()));
    }
}
