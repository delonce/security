package org.security.role.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.security.user.grpc.UserServiceGrpc;
import org.security.user.grpc.UserServiceProto.*;
import org.security.user.grpc.GetRoleResponse;
import org.security.user.grpc.GetRoleByIdRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @RateLimiter(name = "default")
    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    @Cacheable(value = "roles", key = "#p0")
    public GetRoleResponse getRoleById(Long roleId) {
        try {
            GetRoleByIdRequest request = GetRoleByIdRequest.newBuilder()
                    .setId(roleId)
                    .build();

            GetRoleResponse response = userServiceStub.getRoleById(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Role not found: " + response.getErrorMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Error getting role by id via gRPC: {}", roleId, e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
