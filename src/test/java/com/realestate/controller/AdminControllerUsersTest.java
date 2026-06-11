package com.realestate.controller;

import com.realestate.controller.AdminController.AdminUserResponse;
import com.realestate.entity.User;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * /admin/users listing: every role/q combination must go through the single
 * paginated Specification query (the old role-filtered path loaded all rows
 * and ignored the page param), and the response must stay a safe projection.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerUsersTest {

    @Mock private com.realestate.service.PropertyService propertyService;
    @Mock private com.realestate.repository.PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AdminController adminController;

    private User buyer(String name, String email) {
        return User.builder()
            .id(UUID.randomUUID()).name(name).email(email).phone("9876543210")
            .role(User.Role.BUYER).verified(true).active(true)
            .passwordHash("$2a$bcrypt-hash").otpCode("123456")
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_withRoleAndQuery_usesPaginatedSpecificationQuery() {
        Page<User> page = new PageImpl<>(List.of(buyer("Asha", "asha@x.in")), PageRequest.of(2, 20), 41);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AdminUserResponse> result =
            adminController.getUsers("buyer", "asha", 2, 20).getBody();

        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(41); // real total, not page-local size
        assertThat(result.getContent().get(0).email()).isEqualTo("asha@x.in");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_responseIsSafeProjection_withoutCredentialFields() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(buyer("Asha", "asha@x.in"))));

        AdminUserResponse dto =
            adminController.getUsers(null, null, 0, 20).getBody().getContent().get(0);

        // record type can't carry them, but assert the projection contract explicitly
        assertThat(dto.getClass().getRecordComponents())
            .extracting(java.lang.reflect.RecordComponent::getName)
            .doesNotContain("passwordHash", "otpCode", "otpExpiresAt");
    }

    @Test
    void getUsers_invalidRole_throws() {
        assertThatThrownBy(() -> adminController.getUsers("superadmin", null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
