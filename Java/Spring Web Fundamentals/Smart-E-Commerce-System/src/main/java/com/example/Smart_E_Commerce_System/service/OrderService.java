package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.PageResponse; // ADJUST: your PageResponse package
import com.example.Smart_E_Commerce_System.dto.order.CreateOrderRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderListItemResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderSummaryResponse;
import com.example.Smart_E_Commerce_System.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse placeOrder(CreateOrderRequest request);

    OrderResponse getOrder(Integer orderId);

    PageResponse<OrderSummaryResponse> getOrderHistory(Integer userId, Pageable pageable);

    PageResponse<OrderListItemResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);
}