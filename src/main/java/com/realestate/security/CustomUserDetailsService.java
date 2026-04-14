package com.realestate.security;

import com.realestate.entity.User;
import com.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security calls loadUserByUsername() when someone tries to log in.
 * We load the User from the DB and wrap it in a UserDetails object so
 * Spring Security can check the password and assign roles.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // identifier is either an email address or a 10-digit Indian phone number
        boolean isPhone = identifier.matches("^[6-9]\\d{9}$");
        User user = isPhone
            ? userRepository.findByPhone(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with phone: " + identifier))
            : userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with email: " + identifier));

        // Convert our Role enum into Spring Security's GrantedAuthority
        // ROLE_ prefix is required by Spring Security conventions
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(List.of(authority))
            .accountExpired(false)
            .accountLocked(!user.isActive())          // banned users are locked
            .credentialsExpired(false)
            .disabled(!user.isActive())
            .build();
    }
}
