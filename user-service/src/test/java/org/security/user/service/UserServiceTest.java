package org.security.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.security.user.dto.RegisterRequest;
import org.security.user.dto.UserResponse;
import org.security.user.dto.ValidateRequest;
import org.security.user.model.Role;
import org.security.user.model.User;
import org.security.user.model.UserRole;
import org.security.user.repository.RoleRepository;
import org.security.user.repository.UserRepository;
import org.security.user.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void testRegisterSuccess() {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password");
        Role defaultRole = Role.builder().id(1L).name("USER").build();
        User savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));
        when(userRoleRepository.findRoleIdsByUserId(1L)).thenReturn(List.of(1L));

        // When
        UserResponse response = userService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        verify(userRoleRepository).save(any(UserRole.class));
        verify(metricsService).incrementRegistrationAttempts();
        verify(metricsService).incrementSuccessfulRegistrations();
    }

    @Test
    void testRegisterUsernameExists() {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThrows(ResponseStatusException.class, () -> userService.register(request));
        verify(metricsService).incrementFailedRegistrations();
    }

    @Test
    void testValidateCredentialsSuccess() {
        // Given
        ValidateRequest request = new ValidateRequest("testuser", "password");
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded")
                .enabled(true)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        var response = userService.validateCredentials(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.get("id"));
        assertEquals("testuser", response.get("username"));
        verify(metricsService).incrementSuccessfulValidations();
    }

    @Test
    void testValidateCredentialsInvalidPassword() {
        // Given
        ValidateRequest request = new ValidateRequest("testuser", "wrongpassword");
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded")
                .enabled(true)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded")).thenReturn(false);

        // When & Then
        assertThrows(ResponseStatusException.class, () -> userService.validateCredentials(request));
        verify(metricsService).incrementFailedValidations();
    }
}
