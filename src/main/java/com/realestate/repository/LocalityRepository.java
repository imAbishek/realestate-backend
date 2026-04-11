package com.realestate.repository;

import com.realestate.entity.Locality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface LocalityRepository extends JpaRepository<Locality, UUID> {
    List<Locality> findByCityIdAndActiveTrueOrderByNameAsc(UUID cityId);
    Optional<Locality> findBySlugAndCitySlug(String localitySlug, String citySlug);
    List<Locality> findTop10ByNameContainingIgnoreCaseAndActiveTrue(String query);
}
