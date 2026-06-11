package com.realestate.service;

import com.realestate.entity.Property;
import com.realestate.entity.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SavedPropertyRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FavoriteService} — idempotent add + remove semantics.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private SavedPropertyRepository savedRepo;
    @Mock private PropertyRepository      propertyRepo;
    @Mock private UserRepository          userRepo;
    @Mock private PropertyService         propertyService;

    @InjectMocks private FavoriteService favoriteService;

    private static final String EMAIL = "user@propfind.in";

    private User user() {
        return User.builder().id(UUID.randomUUID()).email(EMAIL).build();
    }

    @Test
    void add_newFavorite_savesAndReturnsTrue() {
        User u = user();
        UUID propertyId = UUID.randomUUID();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(propertyRepo.findById(propertyId))
            .thenReturn(Optional.of(Property.builder().id(propertyId).build()));
        when(savedRepo.existsByUserIdAndPropertyId(u.getId(), propertyId)).thenReturn(false);

        assertThat(favoriteService.add(EMAIL, propertyId)).isTrue();
        verify(savedRepo).save(any());
    }

    @Test
    void add_alreadySaved_isNoOpAndReturnsFalse() {
        User u = user();
        UUID propertyId = UUID.randomUUID();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(propertyRepo.findById(propertyId))
            .thenReturn(Optional.of(Property.builder().id(propertyId).build()));
        when(savedRepo.existsByUserIdAndPropertyId(u.getId(), propertyId)).thenReturn(true);

        assertThat(favoriteService.add(EMAIL, propertyId)).isFalse();
        verify(savedRepo, never()).save(any());
    }

    @Test
    void remove_returnsTrueWhenSomethingDeleted() {
        User u = user();
        UUID propertyId = UUID.randomUUID();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(savedRepo.deleteByUserIdAndPropertyId(u.getId(), propertyId)).thenReturn(1);

        assertThat(favoriteService.remove(EMAIL, propertyId)).isTrue();
    }

    @Test
    void remove_returnsFalseWhenNothingDeleted() {
        User u = user();
        UUID propertyId = UUID.randomUUID();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(savedRepo.deleteByUserIdAndPropertyId(u.getId(), propertyId)).thenReturn(0);

        assertThat(favoriteService.remove(EMAIL, propertyId)).isFalse();
    }
}
