package com.example.Smart_E_Commerce_System.dto.order;

import com.example.Smart_E_Commerce_System.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of a customer's order history. Filled directly by a JPQL constructor expression. */
public record OrderSummaryResponse(
        Integer orderId,
        LocalDateTime orderDate,
        OrderStatus status,
        Long itemCount,
        BigDecimal total
) {}
