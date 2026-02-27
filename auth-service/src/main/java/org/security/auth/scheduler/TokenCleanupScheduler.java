package org.security.auth.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.security.auth.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedRate = 3600000) // Каждый час
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            log.info("Starting cleanup of expired tokens");
            refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Cleanup of expired tokens completed");
        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }
}
