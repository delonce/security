package org.security.role.repository;

import org.security.role.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    List<RolePermission> findByRoleId(Long roleId);
    
    @Query("SELECT rp.permissionId FROM RolePermission rp WHERE rp.roleId = ?1")
    List<Long> findPermissionIdsByRoleId(Long roleId);
    
    void deleteByRoleId(Long roleId);
}
