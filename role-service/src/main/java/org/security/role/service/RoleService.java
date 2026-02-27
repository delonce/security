package org.security.role.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.security.role.dto.RoleResponse;
import org.security.role.model.Permission;
import org.security.role.repository.PermissionRepository;
import org.security.role.repository.RolePermissionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.security.role.client.UserServiceGrpcClient;
import org.security.user.grpc.GetRoleResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final MetricsService metricsService;

    @RateLimiter(name = "default")
    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    @Cacheable(value = "permissions", key = "#p0")
    public RoleResponse getRoleWithPermissions(Long roleId) {
        try {
            metricsService.incrementRoleLookupAttempts();
            long startTime = System.currentTimeMillis();

            // Получаем роль из user-service через gRPC
            GetRoleResponse roleResponse = userServiceGrpcClient.getRoleById(roleId);

            if (!roleResponse.getSuccess()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
            }

            // Получаем права доступа для роли
            List<Long> permissionIds = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
            List<String> permissions = permissionIds.stream()
                    .map(permissionId -> permissionRepository.findById(permissionId)
                            .map(Permission::getName)
                            .orElse("UNKNOWN"))
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRoleLookupLatency(duration);
            metricsService.incrementSuccessfulRoleLookups();

            // В реальном приложении здесь была бы полная информация о роли
            return RoleResponse.builder()
                    .id(roleId)
                    .permissions(permissions)
                    .build();
        } catch (Exception e) {
            metricsService.incrementFailedRoleLookups();
            log.error("Error getting role with permissions", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get role");
        }
    }

    @RateLimiter(name = "default")
    @Cacheable(value = "permissions", key = "#p0")
    public Permission getPermissionById(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
    }

    @RateLimiter(name = "default")
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @RateLimiter(name = "default")
    @Cacheable(value = "rolePermissions", key = "#p0")
    public List<String> getPermissionsByRoleId(Long roleId) {
        List<Long> permissionIds = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
        return permissionIds.stream()
                .map(permissionId -> permissionRepository.findById(permissionId)
                        .map(Permission::getName)
                        .orElse("UNKNOWN"))
                .collect(Collectors.toList());
    }
}
