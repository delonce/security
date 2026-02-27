package org.security.role.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void incrementRoleLookupAttempts() {
        Counter.builder("role.lookup.attempts")
                .description("Total number of role lookup attempts")
                .register(meterRegistry)
                .increment();
    }

    public void incrementSuccessfulRoleLookups() {
        Counter.builder("role.lookup.success")
                .description("Total number of successful role lookups")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailedRoleLookups() {
        Counter.builder("role.lookup.failure")
                .description("Total number of failed role lookups")
                .register(meterRegistry)
                .increment();
    }

    public void recordRoleLookupLatency(long milliseconds) {
        Timer.builder("role.lookup.latency")
                .description("Role lookup request latency")
                .register(meterRegistry)
                .record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
