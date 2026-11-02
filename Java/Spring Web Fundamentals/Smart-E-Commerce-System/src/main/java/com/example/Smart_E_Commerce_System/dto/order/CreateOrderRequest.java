package com.example.Smart_E_Commerce_System.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Body of POST /api/orders. @Valid on the list makes each line get validated too. */
public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        Integer userId,

        @NotEmpty(message = "An order needs at least one item")
        List<@Valid OrderItemRequest> items
) {}