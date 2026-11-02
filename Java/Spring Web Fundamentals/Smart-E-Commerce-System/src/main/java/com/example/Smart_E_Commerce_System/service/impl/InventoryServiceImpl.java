package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.exception.InsufficientStockException;
import com.example.Smart_E_Commerce_System.repository.InventoryRepository;
import com.example.Smart_E_Commerce_System.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Propagation.MANDATORY: this method refuses to run on its own.
     * If no transaction is active, Spring throws IllegalTransactionStateException.
     * So stock can only ever change as part of a bigger unit of work (placing an order),
     * and if that bigger unit fails, the stock change is undone with it.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveStock(Integer productId, int quantity) {
        int rowsUpdated = inventoryRepository.decrementStock(productId, quantity);
        if (rowsUpdated == 0) {
            // 0 rows = not enough stock (or no inventory row for this product)
            throw new InsufficientStockException(productId, quantity);
        }
    }
}