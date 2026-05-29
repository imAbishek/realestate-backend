package com.realestate.service;

import com.realestate.dto.property.PropertyDtos.PropertyCardResponse;
import com.realestate.entity.Property;
import com.realestate.entity.SavedProperty;
import com.realestate.entity.User;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.exception.UnauthorizedException;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SavedPropertyRepository;
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
public class FavoriteService {

    private final SavedPropertyRepository savedRepo;
    private final PropertyRepository      propertyRepo;
    private final UserRepository          userRepo;
    private final PropertyService         propertyService;

    /** Idempotent — calling twice with the same (user, property) is a no-op on the 2nd call. */
    @Transactional
    public boolean add(String userEmail, UUID propertyId) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        Property property = propertyRepo.findById(propertyId)
            .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));

        if (savedRepo.existsByUserIdAndPropertyId(user.getId(), propertyId)) {
            return false; // already saved
        }

        SavedProperty saved = SavedProperty.builder()
            .user(user)
            .property(property)
            .build();
        savedRepo.save(saved);
        log.debug("Favorite added: user={} property={}", userEmail, propertyId);
        return true;
    }

    @Transactional
    public boolean remove(String userEmail, UUID propertyId) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        int removed = savedRepo.deleteByUserIdAndPropertyId(user.getId(), propertyId);
        return removed > 0;
    }

    @Transactional(readOnly = true)
    public boolean isSaved(String userEmail, UUID propertyId) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        return savedRepo.existsByUserIdAndPropertyId(user.getId(), propertyId);
    }

    @Transactional(readOnly = true)
    public Page<PropertyCardResponse> listMine(String userEmail, Pageable pageable) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        return savedRepo.findByUserIdOrderBySavedAtDesc(user.getId(), pageable)
            .map(sp -> propertyService.toCardResponse(sp.getProperty()));
    }
}
