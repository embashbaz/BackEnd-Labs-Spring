package com.example.Smart_E_Commerce_System.dto.report;

public record LowStockResponse(Integer productId, String productName, Integer quantityOnHand, Integer reorderLevel) {}
