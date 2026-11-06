package com.example.Smart_E_Commerce_System.repository;


import com.example.Smart_E_Commerce_System.dto.order.OrderSummaryResponse;
import com.example.Smart_E_Commerce_System.entity.Order;
import com.example.Smart_E_Commerce_System.entity.OrderStatus;
import com.example.Smart_E_Commerce_System.repository.projection.MonthlyRevenue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    Page<Order> findByUserId(Integer userId, Pageable pageable);

    // Used before deleting a user (FK is ON DELETE RESTRICT)
    boolean existsByUserId(Integer userId);


    /**
     * Admin view: orders with a given status, paginated.
     * Spring builds the query from the method name; @EntityGraph loads the user
     * in the same SQL query so listing orders does not trigger N+1 selects.
     */
    @EntityGraph(attributePaths = "user")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // ---------- JPQL ----------

    /**
     * Customer order history: one row per order with item count and total,
     * built directly into a DTO (no entities loaded, no lazy-loading).
     * countQuery is given explicitly because Spring cannot derive a correct
     * count from a GROUP BY query. Sortable by orderDate, status.
     */
    @Query(value = """
            select new com.example.Smart_E_Commerce_System.dto.order.OrderSummaryResponse(
                o.id, o.orderDate, o.status, sum(i.quantity), sum(i.unitPrice * i.quantity))
            from Order o join o.items i
            where o.user.id = :userId
            group by o.id, o.orderDate, o.status
            """,
            countQuery = "select count(o) from Order o where o.user.id = :userId")
    Page<OrderSummaryResponse> findOrderHistory(@Param("userId") Integer userId, Pageable pageable);

    /**
     * Order detail: the order, its user, its items and each item's product in ONE query.
     * Fetching a collection is safe here because this returns a single order (no pagination).
     */
    @Query("""
            select o from Order o
            join fetch o.user
            left join fetch o.items i
            left join fetch i.product
            where o.id = :id
            """)
    Optional<Order> findDetailById(@Param("id") Integer id);

    // ---------- Native SQL ----------

    /**
     * Monthly revenue report. Native because date_trunc/to_char are PostgreSQL functions.
     * Aliases are quoted so PostgreSQL keeps their camelCase, which Spring needs
     * to match them to the MonthlyRevenue getters.
     */
    @Query(nativeQuery = true, value = """
            SELECT to_char(date_trunc('month', o.order_date), 'YYYY-MM') AS "month",
                   COUNT(DISTINCT o.order_id)                          AS "orderCount",
                   SUM(oi.quantity * oi.unit_price)                    AS "revenue"
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.order_id
            WHERE o.status <> 'CANCELLED'
              AND o.order_date >= :from
              AND o.order_date <  :to
            GROUP BY 1
            ORDER BY 1
            """)
    List<MonthlyRevenue> findMonthlyRevenue(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /** Changes the status only if it's currently one of 'allowed'. Returns rows changed (0 or 1). */
    @Modifying
    @Query("update Order o set o.status = :newStatus where o.id = :id and o.status in :allowed")
    int updateStatusIfCurrentIn(@Param("id") Integer id,
                                @Param("allowed") Collection<OrderStatus> allowed,
                                @Param("newStatus") OrderStatus newStatus);
}
