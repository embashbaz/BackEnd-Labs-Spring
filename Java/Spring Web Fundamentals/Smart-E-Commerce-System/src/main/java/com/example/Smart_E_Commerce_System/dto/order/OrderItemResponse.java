package com.example.Smart_E_Commerce_System.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Integer productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}