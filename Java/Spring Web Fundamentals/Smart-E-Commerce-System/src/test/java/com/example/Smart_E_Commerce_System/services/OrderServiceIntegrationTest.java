package com.example.Smart_E_Commerce_System.services;

import com.example.Smart_E_Commerce_System.dto.order.CreateOrderRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderItemRequest;
import com.example.Smart_E_Commerce_System.dto.order.OrderResponse;
import com.example.Smart_E_Commerce_System.entity.Category;
import com.example.Smart_E_Commerce_System.entity.Inventory;
import com.example.Smart_E_Commerce_System.entity.OrderStatus;
import com.example.Smart_E_Commerce_System.entity.Product;
import com.example.Smart_E_Commerce_System.entity.User;
import com.example.Smart_E_Commerce_System.exception.InsufficientStockException;
import com.example.Smart_E_Commerce_System.exception.InvalidOrderStatusException;
import com.example.Smart_E_Commerce_System.exception.PaymentDeclinedException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.*;
import com.example.Smart_E_Commerce_System.service.InventoryService;
import com.example.Smart_E_Commerce_System.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test: starts the real application on the H2 "test" profile, so Spring's
 * transaction proxies and a real database are involved.
 *
 * IMPORTANT: this class is deliberately NOT annotated with @Transactional.
 * If it were, the service methods would join the test's transaction and their rollback would
 * be postponed until the test ends, so the checks below would see the changed values and
 * fail even though the code is correct. Without it, each service call runs in its own real
 * transaction, exactly as it does when called from the controller.
 * The trade-off: data is created in @BeforeEach and deleted in @AfterEach by hand.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    private static final int LAPTOP_STOCK = 50;
    private static final int MOUSE_STOCK = 3;
    private static final int MISSING_ID = 999_999;

    @Autowired private OrderService orderService;
    @Autowired private InventoryService inventoryService;

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;

    private User customer;
    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        cleanDatabase(); // start from a known state even if the test profile seeds data

        customer = new User();
        customer.setFullName("Order Test User");
        customer.setEmail("order.test@example.com");
        customer.setPasswordHash("not-a-real-hash");
        customer = userRepository.save(customer);

        Category category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);

        laptop = createProduct(category, "Test Laptop", "999.99", LAPTOP_STOCK);
        mouse = createProduct(category, "Test Mouse", "19.99", MOUSE_STOCK);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    // ============================================================ placing orders

    @Test
    @DisplayName("Valid order is saved and stock is reduced")
    void placeOrder_success() {
        long ordersBefore = orderRepository.count();

        OrderResponse response = orderService.placeOrder(request(
                line(laptop, 2),
                line(mouse, 1)));

        assertThat(response.orderId()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(2);
        assertThat(response.total()).isEqualByComparingTo("2019.97"); // 2 x 999.99 + 1 x 19.99
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK - 2);
        assertThat(stockOf(mouse)).isEqualTo(MOUSE_STOCK - 1);
    }

    @Test
    @DisplayName("Insufficient stock on 2nd item rolls back the 1st item's stock change")
    void placeOrder_insufficientStock_rollsBackEverything() {
        long ordersBefore = orderRepository.count();

        // Laptop is processed first and its stock IS decremented inside the transaction.
        // Mouse then fails (10 requested, only 3 in stock).
        assertThatThrownBy(() -> orderService.placeOrder(request(
                line(laptop, 2),
                line(mouse, 10))))
                .isInstanceOf(InsufficientStockException.class);

        // The rollback undid the laptop decrement and no order was saved
        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK);
        assertThat(stockOf(mouse)).isEqualTo(MOUSE_STOCK);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    @Test
    @DisplayName("Unknown user is rejected and nothing changes")
    void placeOrder_unknownUser_throwsNotFound() {
        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest(
                MISSING_ID, List.of(line(laptop, 1)))))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK);
    }

    @Test
    @DisplayName("Unknown product on 2nd item rolls back the 1st item's stock change")
    void placeOrder_unknownProduct_rollsBack() {
        long ordersBefore = orderRepository.count();

        assertThatThrownBy(() -> orderService.placeOrder(request(
                line(laptop, 2),
                new OrderItemRequest(MISSING_ID, 1))))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    @Test
    @DisplayName("reserveStock (MANDATORY) refuses to run outside a transaction")
    void reserveStock_withoutTransaction_isRejected() {
        // The test has no transaction, so MANDATORY propagation must refuse the call
        assertThatThrownBy(() -> inventoryService.reserveStock(laptop.getId(), 1))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK);
    }

    // ============================================================ payment

    @Test
    @DisplayName("Paying a pending order marks it PAID")
    void payOrder_pending_becomesPaid() {
        Integer orderId = placeOrder(line(laptop, 1));

        OrderResponse paid = orderService.payOrder(orderId);

        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(statusOf(orderId)).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("Declined payment rolls the status back to PENDING")
    void payOrder_declined_rollsBackStatus() {
        // 11 x 999.99 = 10,999.89, above the simulated limit of 10,000
        Integer orderId = placeOrder(line(laptop, 11));

        assertThatThrownBy(() -> orderService.payOrder(orderId))
                .isInstanceOf(PaymentDeclinedException.class);

        // The status WAS set to PAID inside the transaction; the rollback undid it
        assertThat(statusOf(orderId)).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Paying an unknown order is rejected")
    void payOrder_unknownOrder_throwsNotFound() {
        assertThatThrownBy(() -> orderService.payOrder(MISSING_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============================================================ cancellation

    @Test
    @DisplayName("Cancelling an order returns its stock")
    void cancelOrder_restoresStock() {
        Integer orderId = placeOrder(line(laptop, 2), line(mouse, 1));
        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK - 2);

        OrderResponse cancelled = orderService.cancelOrder(orderId);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK);
        assertThat(stockOf(mouse)).isEqualTo(MOUSE_STOCK);
    }

    @Test
    @DisplayName("A paid order can be cancelled and its stock is returned")
    void cancelOrder_paidOrder_restoresStock() {
        Integer orderId = placeOrder(line(laptop, 3));
        orderService.payOrder(orderId);

        orderService.cancelOrder(orderId);

        assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK);
    }

    @Test
    @DisplayName("A cancelled order can't be cancelled again or paid; stock is returned only once")
    void cancelledOrder_rejectsFurtherChanges() {
        Integer orderId = placeOrder(line(laptop, 2));
        orderService.cancelOrder(orderId);

        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(() -> orderService.payOrder(orderId))
                .isInstanceOf(InvalidOrderStatusException.class);

        assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(laptop)).isEqualTo(LAPTOP_STOCK); // not LAPTOP_STOCK + 2
    }

    // ============================================================ helpers

    private Product createProduct(Category category, String name, String price, int stock) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product = productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProduct(product); // @MapsId copies the product's id into the inventory row
        inventory.setQuantityOnHand(stock);
        inventoryRepository.save(inventory);

        return product;
    }

    /** Places an order for the test customer and returns its id. */
    private Integer placeOrder(OrderItemRequest... lines) {
        return orderService.placeOrder(request(lines)).orderId();
    }

    /** Reads the stock fresh from the database (no transaction, so no cached entity). */
    private int stockOf(Product product) {
        return inventoryRepository.findById(product.getId()).orElseThrow().getQuantityOnHand();
    }

    /** Reads the order status fresh from the database. */
    private OrderStatus statusOf(Integer orderId) {
        return orderRepository.findById(orderId).orElseThrow().getStatus();
    }

    private OrderItemRequest line(Product product, int quantity) {
        return new OrderItemRequest(product.getId(), quantity);
    }

    private CreateOrderRequest request(OrderItemRequest... lines) {
        return new CreateOrderRequest(customer.getId(), List.of(lines));
    }

    /** Children before parents, so foreign keys are never violated. */
    private void cleanDatabase() {
        orderRepository.deleteAll();      // order_items are removed with their order (cascade)
        reviewRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
}