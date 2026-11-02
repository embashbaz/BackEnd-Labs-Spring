package com.example.Smart_E_Commerce_System.dto.report;

import java.math.BigDecimal;

public record TopSellingProductResponse(Integer productId, String productName, Long unitsSold, BigDecimal revenue) {}