package com.example.Smart_E_Commerce_System.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Payload for creating or updating a product")
public record ProductRequest(

        @Schema(description = "Id of an existing category", example = "1")
        @NotNull(message = "Category id is required")
        @Positive(message = "Category id must be positive")
        Integer categoryId,

        @Schema(example = "Wireless Mouse")
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters")
        String name,

        @Schema(example = "Ergonomic 2.4 GHz wireless mouse")
        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @Schema(description = "Price with up to 2 decimals", example = "25.99")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimals")
        BigDecimal price,

        @Schema(description = "Starting stock; used on create only, ignored on update", example = "50")
        @PositiveOrZero(message = "Initial stock cannot be negative")
        Integer initialStock
) {}
