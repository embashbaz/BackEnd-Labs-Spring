package com.example.Smart_E_Commerce_System.monitoring;

/** Immutable snapshot of timing statistics for one method, in milliseconds. */
public record MethodStats(
        String method,
        long calls,
        long failures,
        double avgMs,
        double minMs,
        double maxMs,
        double totalMs
) {}
