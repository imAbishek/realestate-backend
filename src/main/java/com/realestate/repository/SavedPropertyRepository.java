package com.realestate.repository;

import com.realestate.entity.SavedProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, UUID> {

    Optional<SavedProperty> findByUserIdAndPropertyId(UUID userId, UUID propertyId);

    boolean existsByUserIdAndPropertyId(UUID userId, UUID propertyId);

    Page<SavedProperty> findByUserIdOrderBySavedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SavedProperty s WHERE s.user.id = :userId AND s.property.id = :propertyId")
    int deleteByUserIdAndPropertyId(@Param("userId") UUID userId, @Param("propertyId") UUID propertyId);
}
