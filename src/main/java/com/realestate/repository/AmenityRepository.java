package com.realestate.repository;

import com.realestate.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    List<Amenity> findAllByOrderByCategoryAscNameAsc();
    List<Amenity> findByCategory(String category);
    List<Amenity> findByIdIn(List<UUID> ids);
}
