package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.OrderItem;
import com.example.Smart_E_Commerce_System.entity.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
}
