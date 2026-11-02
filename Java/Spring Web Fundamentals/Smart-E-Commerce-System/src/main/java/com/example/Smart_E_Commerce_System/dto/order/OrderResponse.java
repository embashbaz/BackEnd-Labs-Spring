package com.example.Smart_E_Commerce_System.dto.order;

import com.example.Smart_E_Commerce_System.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer orderId,
        Integer userId,
        String userName,
        LocalDateTime orderDate,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal total
) {}