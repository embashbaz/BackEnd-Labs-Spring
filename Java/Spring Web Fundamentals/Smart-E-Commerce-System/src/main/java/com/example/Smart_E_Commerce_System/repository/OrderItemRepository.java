package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.OrderItem;
import com.example.Smart_E_Commerce_System.entity.OrderItemId;
import com.example.Smart_E_Commerce_System.repository.projection.TopSellingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
    // ---------- Derived query ----------

    /**
     * True if the product appears in any order.
     * Useful to refuse deleting a product that has order history (return 409).
     */
    boolean existsByProductId(Integer productId);

    // ---------- Native SQL ----------

    /** Best-selling products by units sold, excluding cancelled orders. */
    @Query(nativeQuery = true, value = """
            SELECT p.product_id                     AS "productId",
                   p.name                           AS "productName",
                   SUM(oi.quantity)                 AS "unitsSold",
                   SUM(oi.quantity * oi.unit_price) AS "revenue"
            FROM order_items oi
            JOIN orders   o ON o.order_id   = oi.order_id
            JOIN products p ON p.product_id = oi.product_id
            WHERE o.status <> 'CANCELLED'
            GROUP BY p.product_id, p.name
            ORDER BY "unitsSold" DESC
            LIMIT :limit
            """)
    List<TopSellingProduct> findTopSellingProducts(@Param("limit") int limit);
}
