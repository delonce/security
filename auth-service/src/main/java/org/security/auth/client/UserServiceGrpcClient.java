package org.security.auth.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.security.user.grpc.*;
import org.security.user.grpc.UserServiceProto.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @RateLimiter(name = "default")
    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    @Cacheable(value = "userDetails", key = "#p0")
    public Map<String, Object> validateUserCredentials(String username, String password) {
        try {
            ValidateCredentialsRequest request = ValidateCredentialsRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build();

            ValidateCredentialsResponse response = userServiceStub.validateCredentials(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Validation failed: " + response.getErrorMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", response.getId());
            result.put("username", response.getUsername());
            return result;
        } catch (Exception e) {
            log.error("Error validating user credentials via gRPC", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }

    @RateLimiter(name = "default")
    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public Map<String, Object> getUserById(Long userId) {
        try {
            GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                    .setId(userId)
                    .build();

            GetUserResponse response = userServiceStub.getUserById(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("User not found: " + response.getErrorMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", response.getId());
            result.put("username", response.getUsername());
            result.put("email", response.getEmail());
            result.put("enabled", response.getEnabled());
            return result;
        } catch (Exception e) {
            log.error("Error getting user by id via gRPC: {}", userId, e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
