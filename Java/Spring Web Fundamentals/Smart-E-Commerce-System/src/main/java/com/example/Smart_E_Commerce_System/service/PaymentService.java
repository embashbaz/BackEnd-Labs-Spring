package com.example.Smart_E_Commerce_System.service;

import java.math.BigDecimal;

public interface PaymentService {

    /** Charges the order total. Throws PaymentDeclinedException if the payment is refused. */
    void charge(Integer orderId, BigDecimal amount);
}