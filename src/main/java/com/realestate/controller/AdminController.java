package com.realestate.controller;

import com.realestate.dto.property.PropertyDtos.*;
import com.realestate.entity.User;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.UserRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.service.PropertyService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin-only endpoints.
 *
 * Protected at two levels:
 *  1. SecurityConfig — /admin/** requires ROLE_ADMIN
 *  2. @PreAuthorize on each method — belt and suspenders
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard — listings, users, analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final PropertyService    propertyService;
    private final PropertyRepository propertyRepository;
    private final UserRepository     userRepository;

    // ─────────────────────────────────────────────
    // LISTINGS MANAGEMENT
    // ─────────────────────────────────────────────

    @GetMapping("/listings/pending")
    @Operation(summary = "Get listings awaiting review (oldest first)")
    public ResponseEntity<Page<PropertyCardResponse>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(propertyService.getPendingListings(pageable));
    }

    @GetMapping("/listings/all")
    @Operation(summary = "Get all listings with optional status filter")
    public ResponseEntity<Page<PropertyCardResponse>> getAllListings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // If no status filter, return all
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "createdAt"));

        var listingStatus = status != null
            ? com.realestate.entity.Property.ListingStatus.valueOf(status.toUpperCase())
            : null;
        return ResponseEntity.ok(propertyService.getAllListings(listingStatus, pageable));
    }

    @GetMapping("/listings/{id}")
    @Operation(summary = "Get any listing by ID regardless of status (admin preview)")
    public ResponseEntity<PropertyDetailResponse> getListing(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.getByIdForAdmin(id));
    }

    @PutMapping("/listings/{id}/approve")
    @Operation(summary = "Approve a pending listing (sets status to ACTIVE)")
    public ResponseEntity<PropertyDetailResponse> approve(@PathVariable UUID id) {
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(com.realestate.entity.Property.ListingStatus.ACTIVE);
        return ResponseEntity.ok(propertyService.updateStatus(id, req));
    }

    @PutMapping("/listings/{id}/reject")
    @Operation(summary = "Reject a listing (reason required)")
    public ResponseEntity<PropertyDetailResponse> reject(
            @PathVariable UUID id,
            @RequestBody StatusUpdateRequest req) {
        req.setStatus(com.realestate.entity.Property.ListingStatus.REJECTED);
        return ResponseEntity.ok(propertyService.updateStatus(id, req));
    }

    @PutMapping("/listings/{id}/feature")
    @Operation(summary = "Toggle featured flag on a listing")
    public ResponseEntity<PropertyDetailResponse> toggleFeatured(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.toggleFeatured(id));
    }

    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Admin delete a listing (any listing)")
    public ResponseEntity<Void> adminDelete(
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails admin) {
        propertyService.delete(id, admin.getUsername(), true);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // USER MANAGEMENT
    // ─────────────────────────────────────────────

    /** Safe projection — never exposes passwordHash, otpCode, or otpExpiresAt. */
    public record AdminUserResponse(
        UUID id, String name, String email, String phone,
        User.Role role, boolean isVerified, boolean isActive,
        java.time.LocalDateTime createdAt
    ) {}

    @GetMapping("/users")
    @Operation(summary = "List all users with optional role filter")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "createdAt"));

        if (role != null) {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            List<User> users = userRepository.findAllByRole(userRole);   // single DB call
            List<AdminUserResponse> dtos = users.stream().map(this::toAdminUserResponse).toList();
            return ResponseEntity.ok(new PageImpl<>(dtos, pageable, dtos.size()));
        }
        return ResponseEntity.ok(userRepository.findAll(pageable).map(this::toAdminUserResponse));
    }

    private AdminUserResponse toAdminUserResponse(User u) {
        return new AdminUserResponse(
            u.getId(), u.getName(), u.getEmail(), u.getPhone(),
            u.getRole(), u.isVerified(), u.isActive(), u.getCreatedAt()
        );
    }

    @PutMapping("/users/{id}/ban")
    @Operation(summary = "Ban or unban a user")
    public ResponseEntity<Map<String, Object>> banUser(
            @PathVariable UUID id,
            @RequestParam boolean ban) {

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setActive(!ban);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "userId", id,
            "isActive", user.isActive(),
            "message", ban ? "User banned successfully" : "User reinstated successfully"
        ));
    }

    // ─────────────────────────────────────────────
    // ANALYTICS
    // ─────────────────────────────────────────────

    @GetMapping("/analytics/overview")
    @Operation(summary = "Platform overview stats for the admin dashboard")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalListings",   propertyRepository.count());
        stats.put("activeListings",  propertyRepository.countByStatus(
            com.realestate.entity.Property.ListingStatus.ACTIVE));
        stats.put("pendingReview",   propertyRepository.countByStatus(
            com.realestate.entity.Property.ListingStatus.PENDING_REVIEW));
        stats.put("totalUsers",      userRepository.count());
        stats.put("totalAgents",     userRepository.countByRole(User.Role.AGENT));

        // Top cities by listing count
        List<Map<String, Object>> cityCounts = new ArrayList<>();
        for (Object[] row : propertyRepository.countByCity()) {
            cityCounts.add(Map.of("city", row[0], "count", row[1]));
        }
        stats.put("listingsByCity", cityCounts);

        return ResponseEntity.ok(stats);
    }
}
