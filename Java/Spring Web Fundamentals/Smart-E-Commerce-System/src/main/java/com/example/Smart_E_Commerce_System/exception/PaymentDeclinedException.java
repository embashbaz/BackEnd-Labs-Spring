package com.example.Smart_E_Commerce_System.exception;

import java.math.BigDecimal;

/**
 * The (simulated) payment was refused. Mapped to 402 Payment Required.
 * Unchecked, so the surrounding transaction rolls back and the order stays PENDING.
 */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(Integer orderId, BigDecimal amount, BigDecimal limit) {
        super("Payment declined for order " + orderId + ": amount " + amount + " exceeds the limit of " + limit);
    }
}