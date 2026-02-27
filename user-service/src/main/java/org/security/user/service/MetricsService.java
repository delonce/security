package org.security.user.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void incrementRegistrationAttempts() {
        Counter.builder("user.registration.attempts")
                .description("Total number of registration attempts")
                .register(meterRegistry)
                .increment();
    }

    public void incrementSuccessfulRegistrations() {
        Counter.builder("user.registration.success")
                .description("Total number of successful registrations")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailedRegistrations() {
        Counter.builder("user.registration.failure")
                .description("Total number of failed registrations")
                .register(meterRegistry)
                .increment();
    }

    public void recordRegistrationLatency(long milliseconds) {
        Timer.builder("user.registration.latency")
                .description("Registration request latency")
                .register(meterRegistry)
                .record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void incrementValidationAttempts() {
        Counter.builder("user.validation.attempts")
                .description("Total number of validation attempts")
                .register(meterRegistry)
                .increment();
    }

    public void incrementSuccessfulValidations() {
        Counter.builder("user.validation.success")
                .description("Total number of successful validations")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailedValidations() {
        Counter.builder("user.validation.failure")
                .description("Total number of failed validations")
                .register(meterRegistry)
                .increment();
    }

    public void recordValidationLatency(long milliseconds) {
        Timer.builder("user.validation.latency")
                .description("Validation request latency")
                .register(meterRegistry)
                .record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
