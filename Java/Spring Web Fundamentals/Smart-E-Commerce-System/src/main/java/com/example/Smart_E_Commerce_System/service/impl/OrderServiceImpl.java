package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.config.CacheNames;
import com.example.Smart_E_Commerce_System.dto.PageResponse;
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
import com.example.Smart_E_Commerce_System.exception.InvalidOrderStatusException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.OrderRepository;
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.repository.UserRepository;
import com.example.Smart_E_Commerce_System.service.InventoryService;
import com.example.Smart_E_Commerce_System.service.OrderService;
import com.example.Smart_E_Commerce_System.service.PaymentService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderServiceImpl implements OrderService {

    /** Statuses an order may be in for each action. */
    private static final Set<OrderStatus> PAYABLE = EnumSet.of(OrderStatus.PENDING);
    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            InventoryService inventoryService,
                            PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
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

    /**
     * PENDING -> PAID in one transaction:
     * 1. Change the status with a conditional UPDATE (only if still PENDING).
     * 2. Charge the total. If the payment is declined, PaymentDeclinedException rolls the
     *    status back, so the order stays PENDING and can be paid again later.
     *
     * Reports are not evicted: they count every non-cancelled order, so PENDING -> PAID
     * doesn't change any report result.
     */
    @Override
    @Transactional
    public OrderResponse payOrder(Integer orderId) {
        changeStatus(orderId, PAYABLE, OrderStatus.PAID, "paid");

        OrderResponse response = toResponse(loadDetail(orderId));
        paymentService.charge(orderId, response.total()); // throws -> status change rolled back
        return response;
    }

    /**
     * PENDING or PAID -> CANCELLED in one transaction: the status change and the return of
     * every item's stock succeed or fail together. (A PAID order would also be refunded
     * through the payment provider; the simulation has nothing to refund.)
     *
     * Reports are evicted: cancelled orders are excluded from revenue and top products,
     * and returned stock changes the low-stock list.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.REPORTS, allEntries = true)
    public OrderResponse cancelOrder(Integer orderId) {
        changeStatus(orderId, CANCELLABLE, OrderStatus.CANCELLED, "cancelled");

        Order order = loadDetail(orderId);
        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
        }
        return toResponse(order);
    }

    /** readOnly = true: Hibernate skips checking these entities for changes, which is cheaper. */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Integer orderId) {
        return toResponse(loadDetail(orderId));
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

    /**
     * Changes the status only if the order is currently in one of the allowed statuses,
     * in a single UPDATE (same idea as the stock update). If two requests try to cancel the
     * same order at once, the second one's UPDATE waits for the first to commit, then finds
     * the status already CANCELLED and changes 0 rows, so stock is never returned twice.
     *
     * 0 rows changed means the order doesn't exist (404) or is in the wrong status (409).
     */
    private void changeStatus(Integer orderId, Set<OrderStatus> allowedFrom, OrderStatus newStatus, String action) {
        int rowsUpdated = orderRepository.updateStatusIfCurrentIn(orderId, allowedFrom, newStatus);
        if (rowsUpdated == 0) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
            throw new InvalidOrderStatusException(orderId, order.getStatus(), action);
        }
    }

    private Order loadDetail(Integer orderId) {
        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
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