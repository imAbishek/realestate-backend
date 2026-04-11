package com.realestate.repository;

import com.realestate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA automatically implements all these methods.
 * No SQL needed — method names are the query.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Login + registration lookups
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    // Duplicate checks before registration
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // OTP verification lookup
    Optional<User> findByEmailAndOtpCode(String email, String otpCode);

    // Admin: find all users of a specific role
    java.util.List<User> findAllByRole(User.Role role);

    // Admin analytics: efficient count without loading entities
    long countByRole(User.Role role);

    // Update last login timestamp (avoids loading the full entity)
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :time WHERE u.id = :id")
    void updateLastLogin(UUID id, LocalDateTime time);

    // Clear OTP after verification
    @Modifying
    @Query("UPDATE User u SET u.otpCode = null, u.otpExpiresAt = null WHERE u.id = :id")
    void clearOtp(UUID id);
}
