package org.security.auth.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.security.auth.service.JwtService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class JwtServiceBenchmark {

    private JwtService jwtService;
    private String username = "testuser";
    private Long userId = 1L;

    @Setup
    public void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", 
                "mySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Benchmark
    public String benchmarkGenerateToken() {
        return jwtService.generateToken(username, userId);
    }

    @Benchmark
    public String benchmarkGenerateRefreshToken() {
        return jwtService.generateRefreshToken(username, userId);
    }

    @Benchmark
    public String benchmarkExtractUsername() {
        String token = jwtService.generateToken(username, userId);
        return jwtService.extractUsername(token);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JwtServiceBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
