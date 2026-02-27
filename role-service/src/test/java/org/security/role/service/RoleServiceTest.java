package org.security.role.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.security.role.client.UserServiceGrpcClient;
import org.security.role.model.Permission;
import org.security.role.repository.PermissionRepository;
import org.security.role.repository.RolePermissionRepository;
import org.security.user.grpc.GetRoleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserServiceGrpcClient userServiceGrpcClient;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private RoleService roleService;

    @Test
    void testGetRoleWithPermissionsSuccess() {
        // Given
        Long roleId = 1L;
        Permission permission = Permission.builder()
                .id(1L)
                .name("USER_READ")
                .resource("USER")
                .action("READ")
                .build();

        GetRoleResponse roleResponse = GetRoleResponse.newBuilder()
                .setId(roleId)
                .setName("USER")
                .setSuccess(true)
                .build();

        when(userServiceGrpcClient.getRoleById(roleId))
                .thenReturn(roleResponse);
        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId))
                .thenReturn(List.of(1L));
        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        // When
        var response = roleService.getRoleWithPermissions(roleId);

        // Then
        assertNotNull(response);
        assertEquals(roleId, response.getId());
        assertTrue(response.getPermissions().contains("USER_READ"));
        verify(metricsService).incrementRoleLookupAttempts();
        verify(metricsService).incrementSuccessfulRoleLookups();
        verify(userServiceGrpcClient).getRoleById(roleId);
    }

    @Test
    void testGetPermissionByIdSuccess() {
        // Given
        Long permissionId = 1L;
        Permission permission = Permission.builder()
                .id(permissionId)
                .name("USER_READ")
                .build();

        when(permissionRepository.findById(permissionId))
                .thenReturn(Optional.of(permission));

        // When
        Permission result = roleService.getPermissionById(permissionId);

        // Then
        assertNotNull(result);
        assertEquals(permissionId, result.getId());
        assertEquals("USER_READ", result.getName());
    }

    @Test
    void testGetPermissionByIdNotFound() {
        // Given
        Long permissionId = 999L;

        when(permissionRepository.findById(permissionId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> roleService.getPermissionById(permissionId));
    }
}
