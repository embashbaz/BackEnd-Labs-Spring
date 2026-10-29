package com.example.Smart_E_Commerce_System.aspect;

import com.example.Smart_E_Commerce_System.monitoring.PerformanceMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Times every service method and every REST and GraphQL handler with @Around,
 * records the timings in PerformanceMetrics, and warns about slow calls.
 * Timings are viewable at GET /api/metrics/performance.
 */
@Aspect
@Component
@Order(1) // outermost aspect
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final PerformanceMetrics metrics;
    private final long slowThresholdMs;

    public PerformanceMonitoringAspect(PerformanceMetrics metrics,
                                       @Value("${app.monitoring.slow-threshold-ms:500}") long slowThresholdMs) {
        this.metrics = metrics;
        this.slowThresholdMs = slowThresholdMs;
    }

    @Around("com.example.Smart_E_Commerce_System.aspect.AppPointcuts.serviceLayer()"
            + " || com.example.Smart_E_Commerce_System.aspect.AppPointcuts.restControllers()"
            + " || com.example.Smart_E_Commerce_System.aspect.AppPointcuts.graphqlControllers()")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        String key = layerOf(pjp) + " " + pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();

        long start = System.nanoTime();
        boolean failed = false;
        try {
            return pjp.proceed(); // run the actual method
        } catch (Throwable t) {
            failed = true;
            throw t; // never swallow: the exception handlers still need to see it
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            metrics.record(key, elapsedNanos, failed);

            long elapsedMs = elapsedNanos / 1_000_000;
            if (elapsedMs >= slowThresholdMs) {
                log.warn("SLOW {} took {} ms (threshold {} ms)", key, elapsedMs, slowThresholdMs);
            } else if (log.isDebugEnabled()) {
                log.debug("{} took {} ms", key, String.format("%.2f", elapsedNanos / 1_000_000.0));
            }
        }
    }

    /** Labels each timing so REST, GraphQL and service timings can be compared side by side. */
    private String layerOf(ProceedingJoinPoint pjp) {
        String pkg = pjp.getSignature().getDeclaringType().getPackageName();
        if (pkg.contains(".graphql")) return "[GRAPHQL]";
        if (pkg.contains(".controller")) return "[REST]";
        return "[SERVICE]";
    }
}
