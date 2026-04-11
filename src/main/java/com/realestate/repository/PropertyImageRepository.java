package com.realestate.repository;

import com.realestate.entity.PropertyImage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

    List<PropertyImage> findByPropertyIdOrderBySortOrderAsc(UUID propertyId);
    void deleteByPropertyId(UUID propertyId);

    @Modifying
    @Query("UPDATE PropertyImage i SET i.isPrimary = false WHERE i.property.id = :propertyId")
    void clearPrimaryFlag(@Param("propertyId") UUID propertyId);

    long countByPropertyId(UUID propertyId);
}
