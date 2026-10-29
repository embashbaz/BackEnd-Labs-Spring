package com.example.Smart_E_Commerce_System.controller;

import com.example.Smart_E_Commerce_System.dto.ApiResponse;
import com.example.Smart_E_Commerce_System.monitoring.MethodStats;
import com.example.Smart_E_Commerce_System.monitoring.PerformanceMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Monitoring", description = "Method timings collected by the AOP performance aspect")
public class MetricsController {

    private final PerformanceMetrics metrics;

    public MetricsController(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/performance")
    @Operation(summary = "Timing statistics per method",
            description = "Covers service methods and REST/GraphQL handlers, slowest average first")
    public ResponseEntity<ApiResponse<List<MethodStats>>> performance() {
        return ResponseEntity.ok(ApiResponse.success("Performance metrics", metrics.snapshot()));
    }

    @DeleteMapping("/performance")
    @Operation(summary = "Reset all timing statistics", description = "Use before a benchmark run")
    public ResponseEntity<ApiResponse<Void>> reset() {
        metrics.reset();
        return ResponseEntity.ok(ApiResponse.success("Performance metrics reset", null));
    }
}
