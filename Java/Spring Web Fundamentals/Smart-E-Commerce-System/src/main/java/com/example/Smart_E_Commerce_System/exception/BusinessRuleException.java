package com.example.Smart_E_Commerce_System.exception;

/** Thrown when a request is valid but breaks a business rule (e.g. deleting a category in use). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
