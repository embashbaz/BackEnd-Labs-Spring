package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.config.CacheNames;
import com.example.Smart_E_Commerce_System.dto.PageResponse; // ADJUST: your PageResponse package
import com.example.Smart_E_Commerce_System.dto.order.CreateOrderRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderItemRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderItemResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderListItemResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderResponse;
import com.example.Smart_E_Commerce_System.dto.order.OrderSummaryResponse;
import com.example.Smart_E_Commerce_System.entity.Order;
import com.example.Smart_E_Commerce_System.entity.OrderItem;
import com.example.Smart_E_Commerce_System.entity.OrderStatus;
import com.example.Smart_E_Commerce_System.entity.Product;
import com.example.Smart_E_Commerce_System.entity.User;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.OrderRepository;
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.repository.UserRepository;
import com.example.Smart_E_Commerce_System.service.InventoryService;
import com.example.Smart_E_Commerce_System.service.OrderService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Everything in this method is ONE transaction: all of it is saved, or none of it.
     *
     * - Propagation: REQUIRED (the default). Joins a transaction if one exists, otherwise
     *   starts a new one. InventoryService.reserveStock (MANDATORY) joins this one.
     * - Isolation: READ_COMMITTED (PostgreSQL's default, stated explicitly). We only ever
     *   see committed data from other transactions. That is enough here because the stock
     *   check and decrement happen in a single UPDATE, so two buyers can't both take the last item.
     * - Rollback: any RuntimeException (404, InsufficientStockException) undoes every
     *   change made so far, including stock already removed for earlier lines.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(cacheNames = CacheNames.REPORTS, allEntries = true)
    public OrderResponse placeOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        // order_items has a composite key (order_id, product_id), so the same product
        // can appear only once per order: merge duplicate lines by adding their quantities.
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest line : request.items()) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        Order order = new Order();
        order.setUser(user);

        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Integer productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

            // Throws InsufficientStockException -> whole transaction rolls back
            inventoryService.reserveStock(productId, quantity);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice()); // price at time of purchase
            order.addItem(item);
        }

        // Saving the order also saves its items (cascade = ALL on Order.items)
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    /** readOnly = true: Hibernate skips checking these entities for changes, which is cheaper. */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Integer orderId) {
        return orderRepository.findDetailById(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order",orderId));
    }

    /**
     * A customer's order history. The query already returns DTOs (JPQL constructor
     * expression), so there is nothing to map: just wrap the page.
     * Checks the user first so an unknown user gives 404 instead of an empty page.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrderHistory(Integer userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return PageResponse.from(orderRepository.findOrderHistory(userId, pageable));
    }

    /** Admin list. findByStatus loads each order's user in the same query (@EntityGraph). */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderListItemResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<OrderListItemResponse> page = orderRepository.findByStatus(status, pageable)
                .map(order -> new OrderListItemResponse(
                        order.getId(),
                        order.getUser().getId(),
                        order.getUser().getFullName(),
                        order.getOrderDate(),
                        order.getStatus()));
        return PageResponse.from(page);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();

        BigDecimal total = items.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getOrderDate(),
                order.getStatus(),
                items,
                total);
    }
}