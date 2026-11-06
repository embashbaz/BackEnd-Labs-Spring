package com.example.Smart_E_Commerce_System.exception;

import com.example.Smart_E_Commerce_System.entity.OrderStatus;

/** The order exists but its current status doesn't allow the action (e.g. paying a cancelled order). 409. */
public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(Integer orderId, OrderStatus currentStatus, String action) {
        super("Order " + orderId + " cannot be " + action + ": its status is " + currentStatus);
    }
}