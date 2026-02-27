package org.security.role.controller;

import lombok.RequiredArgsConstructor;
import org.security.role.dto.RoleResponse;
import org.security.role.model.Permission;
import org.security.role.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getRoleWithPermissions(@PathVariable("roleId") Long roleId) {
        return ResponseEntity.ok(roleService.getRoleWithPermissions(roleId));
    }

    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<List<String>> getPermissionsByRoleId(@PathVariable("roleId") Long roleId) {
        return ResponseEntity.ok(roleService.getPermissionsByRoleId(roleId));
    }

    @GetMapping("/permissions/{permissionId}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable("permissionId") Long permissionId) {
        return ResponseEntity.ok(roleService.getPermissionById(permissionId));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }
}
