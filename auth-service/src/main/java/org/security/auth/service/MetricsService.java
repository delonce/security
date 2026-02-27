package org.security.auth.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void incrementLoginAttempts() {
        Counter.builder("auth.login.attempts")
                .description("Total number of login attempts")
                .register(meterRegistry)
                .increment();
    }

    public void incrementSuccessfulLogins() {
        Counter.builder("auth.login.success")
                .description("Total number of successful logins")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailedLogins() {
        Counter.builder("auth.login.failure")
                .description("Total number of failed logins")
                .register(meterRegistry)
                .increment();
    }

    public void recordLoginLatency(long milliseconds) {
        Timer.builder("auth.login.latency")
                .description("Login request latency")
                .register(meterRegistry)
                .record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void incrementRefreshAttempts() {
        Counter.builder("auth.refresh.attempts")
                .description("Total number of token refresh attempts")
                .register(meterRegistry)
                .increment();
    }

    public void incrementSuccessfulRefreshes() {
        Counter.builder("auth.refresh.success")
                .description("Total number of successful token refreshes")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailedRefreshes() {
        Counter.builder("auth.refresh.failure")
                .description("Total number of failed token refreshes")
                .register(meterRegistry)
                .increment();
    }

    public void recordRefreshLatency(long milliseconds) {
        Timer.builder("auth.refresh.latency")
                .description("Token refresh latency")
                .register(meterRegistry)
                .record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
