package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.exception.PaymentDeclinedException;
import com.example.Smart_E_Commerce_System.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Stand-in for a real payment provider. It declines any amount above a configurable
 * limit (app.payment.max-amount, default 10,000), which gives a realistic, repeatable
 * failure case for testing rollback. No card data is involved.
 *
 * In a real system this would call an external provider. That call can't be rolled back
 * by a database transaction, so real integrations use the provider's idempotency keys
 * and confirm the payment before marking the order PAID.
 */
@Service
public class SimulatedPaymentService implements PaymentService {

    private final BigDecimal maxAmount;

    public SimulatedPaymentService(@Value("${app.payment.max-amount:10000}") BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public void charge(Integer orderId, BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            throw new PaymentDeclinedException(orderId, amount, maxAmount);
        }
        // Payment accepted: nothing else to simulate.
    }
}