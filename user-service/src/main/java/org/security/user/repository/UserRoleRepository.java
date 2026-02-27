package org.security.user.repository;

import org.security.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    
    List<UserRole> findByUserId(Long userId);
    
    @Query("SELECT ur.roleId FROM UserRole ur WHERE ur.userId = ?1")
    List<Long> findRoleIdsByUserId(Long userId);
    
    void deleteByUserId(Long userId);
}
