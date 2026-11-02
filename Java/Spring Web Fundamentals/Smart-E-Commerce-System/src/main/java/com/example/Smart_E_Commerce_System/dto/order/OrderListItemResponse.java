package com.example.Smart_E_Commerce_System.dto.order;

import com.example.Smart_E_Commerce_System.entity.OrderStatus;

import java.time.LocalDateTime;

/** One row of the admin order list (no items/total, so no extra queries per order). */
public record OrderListItemResponse(
        Integer orderId,
        Integer userId,
        String userName,
        LocalDateTime orderDate,
        OrderStatus status
) {}