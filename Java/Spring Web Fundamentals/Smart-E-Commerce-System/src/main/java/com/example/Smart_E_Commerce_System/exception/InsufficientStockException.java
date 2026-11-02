package com.example.Smart_E_Commerce_System.exception;

/**
 * Thrown when a product does not have enough stock for an order.
 * Extends RuntimeException on purpose: Spring rolls a transaction back
 * automatically only for unchecked (runtime) exceptions.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Integer productId, int requested) {
        super("Not enough stock for product " + productId + " (requested " + requested + ")");
    }
}
