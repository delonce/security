package org.security.role.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PermissionLookupBenchmark {

    private List<String> permissions;
    private String targetPermission = "USER_READ";

    @Setup
    public void setup() {
        permissions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            permissions.add("PERMISSION_" + i);
        }
        permissions.add(targetPermission);
    }

    @Benchmark
    public boolean benchmarkContains() {
        return permissions.contains(targetPermission);
    }

    @Benchmark
    public boolean benchmarkStreamAnyMatch() {
        return permissions.stream().anyMatch(p -> p.equals(targetPermission));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PermissionLookupBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
