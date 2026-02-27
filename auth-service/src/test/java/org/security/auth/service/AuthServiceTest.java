package org.security.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.security.auth.client.UserServiceGrpcClient;
import org.security.auth.dto.LoginRequest;
import org.security.auth.dto.RefreshTokenRequest;
import org.security.auth.dto.TokenResponse;
import org.security.auth.model.RefreshToken;
import org.security.auth.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserServiceGrpcClient userServiceGrpcClient;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testLoginSuccess() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "password");
        Map<String, Object> userResponse = Map.of(
                "id", 1L,
                "username", "testuser"
        );

        when(userServiceGrpcClient.validateUserCredentials(anyString(), anyString()))
                .thenReturn(userResponse);
        when(jwtService.generateToken(anyString(), anyLong()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), anyLong()))
                .thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        // When
        TokenResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(metricsService).incrementLoginAttempts();
        verify(metricsService).incrementSuccessfulLogins();
        verify(userServiceGrpcClient).validateUserCredentials("testuser", "password");
    }

    @Test
    void testLoginFailure() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(userServiceGrpcClient.validateUserCredentials(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        assertThrows(ResponseStatusException.class, () -> authService.login(request));
        verify(metricsService).incrementLoginAttempts();
        verify(metricsService).incrementFailedLogins();
        verify(userServiceGrpcClient).validateUserCredentials("testuser", "wrongpassword");
    }

    @Test
    void testRefreshTokenSuccess() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        Map<String, Object> userResponse = Map.of(
                "id", 1L,
                "username", "testuser",
                "email", "test@example.com",
                "enabled", true
        );

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(userServiceGrpcClient.getUserById(1L))
                .thenReturn(userResponse);
        when(jwtService.generateToken(anyString(), anyLong()))
                .thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(anyString(), anyLong()))
                .thenReturn("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        // When
        TokenResponse response = authService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository).save(argThat(token -> token.getRevoked()));
        verify(metricsService).incrementRefreshAttempts();
        verify(metricsService).incrementSuccessfulRefreshes();
        verify(userServiceGrpcClient).getUserById(1L);
    }

    @Test
    void testRefreshTokenInvalid() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        when(refreshTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> authService.refreshToken(request));
        verify(metricsService).incrementRefreshAttempts();
        verify(metricsService).incrementFailedRefreshes();
    }
}
