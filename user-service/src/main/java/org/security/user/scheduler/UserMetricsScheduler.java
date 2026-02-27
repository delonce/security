package org.security.user.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.security.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMetricsScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void logUserStatistics() {
        try {
            long totalUsers = userRepository.count();
            log.info("Total users in system: {}", totalUsers);
        } catch (Exception e) {
            log.error("Error logging user statistics", e);
        }
    }
}
