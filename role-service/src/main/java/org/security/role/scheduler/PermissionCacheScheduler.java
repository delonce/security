package org.security.role.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.security.role.repository.PermissionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheScheduler {

    private final PermissionRepository permissionRepository;

    @Scheduled(fixedRate = 1800000) // Каждые 30 минут
    @CacheEvict(value = {"permissions", "rolePermissions"}, allEntries = true)
    public void evictPermissionCache() {
        try {
            log.info("Evicting permission cache");
            long count = permissionRepository.count();
            log.info("Total permissions in system: {}", count);
        } catch (Exception e) {
            log.error("Error during cache eviction", e);
        }
    }
}
