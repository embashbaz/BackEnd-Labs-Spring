package com.example.Smart_E_Commerce_System.monitoring;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe, in-memory store of method timings collected by PerformanceMonitoringAspect.
 * Uses lock-free counters so recording a timing adds almost no overhead under concurrent load.
 */
@Component
public class PerformanceMetrics {

    private final Map<String, Accumulator> byMethod = new ConcurrentHashMap<>();

    public void record(String method, long elapsedNanos, boolean failed) {
        byMethod.computeIfAbsent(method, k -> new Accumulator()).add(elapsedNanos, failed);
    }

    /** All methods, slowest average first. */
    public List<MethodStats> snapshot() {
        return byMethod.entrySet().stream()
                .map(e -> e.getValue().toStats(e.getKey()))
                .sorted(Comparator.comparingDouble(MethodStats::avgMs).reversed())
                .toList();
    }

    public void reset() {
        byMethod.clear();
    }

    private static final class Accumulator {
        private final LongAdder calls = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxNanos = new AtomicLong(0);

        void add(long nanos, boolean failed) {
            calls.increment();
            if (failed) failures.increment();
            totalNanos.add(nanos);
            minNanos.accumulateAndGet(nanos, Math::min);
            maxNanos.accumulateAndGet(nanos, Math::max);
        }

        MethodStats toStats(String method) {
            long n = calls.sum();
            long total = totalNanos.sum();
            return new MethodStats(
                    method,
                    n,
                    failures.sum(),
                    round(n == 0 ? 0 : total / (double) n / 1_000_000),
                    round(n == 0 ? 0 : minNanos.get() / 1_000_000.0),
                    round(maxNanos.get() / 1_000_000.0),
                    round(total / 1_000_000.0)
            );
        }

        private static double round(double ms) {
            return Math.round(ms * 100) / 100.0;
        }
    }
}
