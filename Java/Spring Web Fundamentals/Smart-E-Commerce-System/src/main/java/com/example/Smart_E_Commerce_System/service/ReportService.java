package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.report.LowStockResponse;
import com.example.Smart_E_Commerce_System.dto.report.MonthlyRevenueResponse;
import com.example.Smart_E_Commerce_System.dto.report.TopSellingProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    List<MonthlyRevenueResponse> getMonthlyRevenue(LocalDate from, LocalDate to);

    List<TopSellingProductResponse> getTopSellingProducts(int limit);

    List<LowStockResponse> getLowStockProducts();
}