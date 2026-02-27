package org.security.auth.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.security.auth.dto.LoginRequest;
import org.security.auth.dto.RefreshTokenRequest;
import org.security.auth.dto.TokenResponse;
import org.security.auth.model.RefreshToken;
import org.security.auth.repository.RefreshTokenRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.security.auth.client.UserServiceGrpcClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final MetricsService metricsService;

    @RateLimiter(name = "default")
    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            metricsService.incrementLoginAttempts();
            long startTime = System.currentTimeMillis();

            // Вызов user-service для проверки учетных данных
            Map<String, Object> userResponse = validateUserCredentials(request.getUsername(), request.getPassword());
            
            Long userId = Long.valueOf(userResponse.get("id").toString());
            String username = userResponse.get("username").toString();

            String accessToken = jwtService.generateToken(username, userId);
            String refreshToken = jwtService.generateRefreshToken(username, userId);

            // Сохраняем refresh token
            RefreshToken token = RefreshToken.builder()
                    .token(refreshToken)
                    .userId(userId)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(token);

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordLoginLatency(duration);
            metricsService.incrementSuccessfulLogins();

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(86400L) // 24 hours
                    .build();
        } catch (Exception e) {
            metricsService.incrementFailedLogins();
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @RateLimiter(name = "default")
    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            metricsService.incrementRefreshAttempts();
            long startTime = System.currentTimeMillis();

            RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

            if (refreshToken.getRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
            }

            // Получаем информацию о пользователе
            Map<String, Object> userResponse = getUserById(refreshToken.getUserId());
            String username = userResponse.get("username").toString();

            // Генерируем новые токены
            String newAccessToken = jwtService.generateToken(username, refreshToken.getUserId());
            String newRefreshToken = jwtService.generateRefreshToken(username, refreshToken.getUserId());

            // Отзываем старый токен
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);

            // Сохраняем новый refresh token
            RefreshToken newToken = RefreshToken.builder()
                    .token(newRefreshToken)
                    .userId(refreshToken.getUserId())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(newToken);

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRefreshLatency(duration);
            metricsService.incrementSuccessfulRefreshes();

            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(86400L)
                    .build();
        } catch (ResponseStatusException e) {
            metricsService.incrementFailedRefreshes();
            throw e;
        } catch (Exception e) {
            metricsService.incrementFailedRefreshes();
            log.error("Token refresh failed", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token refresh failed");
        }
    }

    @Cacheable(value = "userDetails", key = "#p0")
    private Map<String, Object> validateUserCredentials(String username, String password) {
        try {
            return userServiceGrpcClient.validateUserCredentials(username, password);
        } catch (Exception e) {
            log.error("Error validating user credentials", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service unavailable");
        }
    }

    private Map<String, Object> getUserById(Long userId) {
        try {
            return userServiceGrpcClient.getUserById(userId);
        } catch (Exception e) {
            log.error("Error getting user by id", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service unavailable");
        }
    }
}
