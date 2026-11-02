package com.example.Smart_E_Commerce_System.service;

public interface InventoryService {
    /** Removes stock for an order. Must be called inside an existing transaction. */
    void reserveStock(Integer productId, int quantity);
}
