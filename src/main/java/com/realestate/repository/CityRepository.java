package com.realestate.repository;

import com.realestate.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {
    List<City> findByActiveTrueOrderByNameAsc();
    Optional<City> findBySlug(String slug);
}
