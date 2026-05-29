package com.realestate.repository;

import com.realestate.entity.SiteVisitBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SiteVisitBookingRepository extends JpaRepository<SiteVisitBooking, UUID> {

    Page<SiteVisitBooking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<SiteVisitBooking> findByPropertyOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);
}
