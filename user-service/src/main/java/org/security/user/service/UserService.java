package org.security.user.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.security.user.dto.RegisterRequest;
import org.security.user.dto.UserResponse;
import org.security.user.dto.ValidateRequest;
import org.security.user.model.Role;
import org.security.user.model.User;
import org.security.user.model.UserRole;
import org.security.user.repository.RoleRepository;
import org.security.user.repository.UserRepository;
import org.security.user.repository.UserRoleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MetricsService metricsService;

    @RateLimiter(name = "default")
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse register(RegisterRequest request) {
        try {
            metricsService.incrementRegistrationAttempts();
            long startTime = System.currentTimeMillis();

            if (userRepository.existsByUsername(request.getUsername())) {
                metricsService.incrementFailedRegistrations();
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                metricsService.incrementFailedRegistrations();
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);

            // Назначаем роль USER по умолчанию
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found"));

            UserRole userRole = UserRole.builder()
                    .userId(user.getId())
                    .roleId(defaultRole.getId())
                    .build();
            userRoleRepository.save(userRole);

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRegistrationLatency(duration);
            metricsService.incrementSuccessfulRegistrations();

            return mapToUserResponse(user);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            metricsService.incrementFailedRegistrations();
            log.error("Registration failed for user: {}", request.getUsername(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    @RateLimiter(name = "default")
    @Cacheable(value = "users", key = "#p0")
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToUserResponse(user);
    }

    @RateLimiter(name = "default")
    @Cacheable(value = "users", key = "#p0")
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToUserResponse(user);
    }

    @RateLimiter(name = "default")
    public Map<String, Object> validateCredentials(ValidateRequest request) {
        try {
            metricsService.incrementValidationAttempts();
            long startTime = System.currentTimeMillis();

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        metricsService.incrementFailedValidations();
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                    });

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                metricsService.incrementFailedValidations();
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }

            if (!user.getEnabled()) {
                metricsService.incrementFailedValidations();
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is disabled");
            }

            // Обновляем время последнего входа
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordValidationLatency(duration);
            metricsService.incrementSuccessfulValidations();

            return Map.of(
                    "id", user.getId(),
                    "username", user.getUsername()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            metricsService.incrementFailedValidations();
            log.error("Validation failed for user: {}", request.getUsername(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @RateLimiter(name = "default")
    @Cacheable(value = "userRoles", key = "#p0")
    public List<String> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        return roleIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .map(Role::getName)
                        .orElse("UNKNOWN"))
                .collect(Collectors.toList());
    }

    @RateLimiter(name = "default")
    @Cacheable(value = "roles", key = "#p0")
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private UserResponse mapToUserResponse(User user) {
        List<String> roles = getUserRoles(user.getId());
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .roles(roles)
                .build();
    }
}
