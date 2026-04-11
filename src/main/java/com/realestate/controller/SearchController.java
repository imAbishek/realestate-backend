package com.realestate.controller;

import com.realestate.entity.City;
import com.realestate.entity.Locality;
import com.realestate.entity.Amenity;
import com.realestate.repository.CityRepository;
import com.realestate.repository.LocalityRepository;
import com.realestate.repository.AmenityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Search-support endpoints.
 * Cities, localities, amenities, autocomplete.
 * All public — no auth needed.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Location lookup and autocomplete")
public class SearchController {

    private final CityRepository     cityRepository;
    private final LocalityRepository localityRepository;
    private final AmenityRepository  amenityRepository;

    @GetMapping("/cities")
    @Operation(summary = "List all active cities")
    public ResponseEntity<List<City>> getCities() {
        return ResponseEntity.ok(cityRepository.findByActiveTrueOrderByNameAsc());
    }

    @GetMapping("/localities")
    @Operation(summary = "List localities in a city")
    public ResponseEntity<List<Locality>> getLocalities(
            @RequestParam UUID cityId) {
        return ResponseEntity.ok(
            localityRepository.findByCityIdAndActiveTrueOrderByNameAsc(cityId)
        );
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Locality autocomplete for search bar")
    public ResponseEntity<List<Locality>> autocomplete(
            @RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(
            localityRepository.findTop10ByNameContainingIgnoreCaseAndActiveTrue(q.trim())
        );
    }

    @GetMapping("/amenities")
    @Operation(summary = "List all amenities (for the property form checkboxes)")
    public ResponseEntity<List<Amenity>> getAmenities() {
        return ResponseEntity.ok(amenityRepository.findAllByOrderByCategoryAscNameAsc());
    }
}
