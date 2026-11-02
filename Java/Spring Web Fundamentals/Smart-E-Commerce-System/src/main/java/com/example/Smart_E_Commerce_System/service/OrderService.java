package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.order.CreateOrderRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderResponse;

public interface OrderService {

    OrderResponse placeOrder(CreateOrderRequest request);

    OrderResponse getOrder(Integer orderId);
}