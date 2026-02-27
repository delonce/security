package org.security.user.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PasswordEncoderBenchmark {

    private PasswordEncoder passwordEncoder;
    private String password = "testPassword123";
    private String encodedPassword;

    @Setup
    public void setup() {
        passwordEncoder = new BCryptPasswordEncoder();
        encodedPassword = passwordEncoder.encode(password);
    }

    @Benchmark
    public String benchmarkEncode() {
        return passwordEncoder.encode(password);
    }

    @Benchmark
    public boolean benchmarkMatches() {
        return passwordEncoder.matches(password, encodedPassword);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PasswordEncoderBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
