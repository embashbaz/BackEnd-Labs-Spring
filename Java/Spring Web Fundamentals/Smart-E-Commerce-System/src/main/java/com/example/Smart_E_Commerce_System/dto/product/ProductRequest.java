package com.example.Smart_E_Commerce_System.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotNull(message = "Category id is required")
        @Positive(message = "Category id must be positive")
        Integer categoryId,

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters")
        String name,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimals")
        BigDecimal price,

        // Only used on create; ignored on update (stock is managed via inventory)
        @PositiveOrZero(message = "Initial stock cannot be negative")
        Integer initialStock
) {}
