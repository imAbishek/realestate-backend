package com.realestate.controller;

import com.realestate.dto.property.PropertyDtos.PropertyCardResponse;
import com.realestate.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Saved properties ("favorites"). All endpoints require a valid JWT —
 * the user identity comes from the security context, never from the request body.
 */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Save / unsave properties")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{propertyId}")
    @Operation(summary = "Save (favorite) a property — idempotent")
    public ResponseEntity<Map<String, Object>> add(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal UserDetails currentUser) {
        boolean added = favoriteService.add(currentUser.getUsername(), propertyId);
        return ResponseEntity.status(added ? HttpStatus.CREATED : HttpStatus.OK)
            .body(Map.of("saved", true, "alreadySaved", !added));
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Remove a property from favorites")
    public ResponseEntity<Map<String, Object>> remove(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal UserDetails currentUser) {
        boolean removed = favoriteService.remove(currentUser.getUsername(), propertyId);
        return ResponseEntity.ok(Map.of("saved", false, "wasSaved", removed));
    }

    @GetMapping("/{propertyId}")
    @Operation(summary = "Check whether a property is in the current user's favorites")
    public ResponseEntity<Map<String, Boolean>> check(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(Map.of(
            "saved", favoriteService.isSaved(currentUser.getUsername(), propertyId)
        ));
    }

    @GetMapping
    @Operation(summary = "List the current user's saved properties")
    public ResponseEntity<Page<PropertyCardResponse>> listMine(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails currentUser) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(favoriteService.listMine(currentUser.getUsername(), pageable));
    }
}
