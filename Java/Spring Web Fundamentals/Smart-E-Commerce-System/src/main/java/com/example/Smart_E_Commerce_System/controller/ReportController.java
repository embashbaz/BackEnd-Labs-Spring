package com.example.Smart_E_Commerce_System.controller;

import com.example.Smart_E_Commerce_System.dto.ApiResponse;
import com.example.Smart_E_Commerce_System.dto.report.LowStockResponse;
import com.example.Smart_E_Commerce_System.dto.report.MonthlyRevenueResponse;
import com.example.Smart_E_Commerce_System.dto.report.TopSellingProductResponse;
import com.example.Smart_E_Commerce_System.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Sales and inventory reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly-revenue")
    @Operation(summary = "Revenue per month (native SQL)",
            description = "Excludes cancelled orders. Dates are yyyy-MM-dd, both inclusive. Default: last 12 months.")
    public ResponseEntity<ApiResponse<List<MonthlyRevenueResponse>>> monthlyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<MonthlyRevenueResponse> report = reportService.getMonthlyRevenue(from, to);
        return ResponseEntity.ok(ApiResponse.success( "Monthly revenue retrieved successfully", report));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Best-selling products (native SQL)", description = "limit between 1 and 100, default 10.")
    public ResponseEntity<ApiResponse<List<TopSellingProductResponse>>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopSellingProductResponse> report = reportService.getTopSellingProducts(limit);
        return ResponseEntity.ok(ApiResponse.success( "Top products retrieved successfully", report));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Products at or below their reorder level (JPQL)")
    public ResponseEntity<ApiResponse<List<LowStockResponse>>> lowStock() {
        List<LowStockResponse> report = reportService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success( "Low-stock products retrieved successfully", report));
    }
}