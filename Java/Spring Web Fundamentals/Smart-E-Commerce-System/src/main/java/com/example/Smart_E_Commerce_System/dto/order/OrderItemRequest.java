package com.example.Smart_E_Commerce_System.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One line of a new order: which product and how many. */
public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        Integer productId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be at least 1")
        Integer quantity
) {}