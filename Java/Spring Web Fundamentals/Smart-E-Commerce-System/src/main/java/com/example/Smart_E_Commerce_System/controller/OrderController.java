package com.example.Smart_E_Commerce_System.controller;

import com.example.Smart_E_Commerce_System.dto.ApiResponse;   // ADJUST: your ApiResponse package
import com.example.Smart_E_Commerce_System.dto.PageResponse;  // ADJUST: your PageResponse package
import com.example.Smart_E_Commerce_System.dto.order.CreateOrderRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderListItemResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderSummaryResponse;
import com.example.Smart_E_Commerce_System.entity.OrderStatus;
import com.example.Smart_E_Commerce_System.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Orders", description = "Place and view orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @Operation(summary = "Place an order",
            description = "Creates the order and removes stock in one transaction. "
                    + "Returns 409 and changes nothing if any product lacks stock.")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body( ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get an order with its items")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Integer id) {
        OrderResponse order = orderService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.success( "Order retrieved successfully", order));
    }

    @GetMapping("/orders")
    @Operation(summary = "List orders by status (admin)",
            description = "Paginated. Sortable by orderDate, status, id. Default: newest first.")
    public ResponseEntity<ApiResponse<PageResponse<OrderListItemResponse>>> getOrdersByStatus(
            @RequestParam OrderStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        PageResponse<OrderListItemResponse> page = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", page));
    }

    @GetMapping("/users/{userId}/orders")
    @Operation(summary = "A customer's order history",
            description = "Paginated, one row per order with item count and total. "
                    + "Sortable by orderDate or status. Default: newest first.")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrderHistory(
            @PathVariable Integer userId,
            @ParameterObject @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        PageResponse<OrderSummaryResponse> page = orderService.getOrderHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Order history retrieved successfully", page));
    }
}