package com.example.Smart_E_Commerce_System.dto.report;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(String month, Long orderCount, BigDecimal revenue) {}