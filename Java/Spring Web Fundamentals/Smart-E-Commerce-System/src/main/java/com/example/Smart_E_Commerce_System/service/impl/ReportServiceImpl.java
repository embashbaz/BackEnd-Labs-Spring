package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.config.CacheNames;
import com.example.Smart_E_Commerce_System.dto.report.LowStockResponse;
import com.example.Smart_E_Commerce_System.dto.report.MonthlyRevenueResponse;
import com.example.Smart_E_Commerce_System.dto.report.TopSellingProductResponse;
import com.example.Smart_E_Commerce_System.repository.InventoryRepository;
import com.example.Smart_E_Commerce_System.repository.OrderItemRepository;
import com.example.Smart_E_Commerce_System.repository.OrderRepository;
import com.example.Smart_E_Commerce_System.service.ReportService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only reports. The native-query projections are copied into records here,
 * so the API returns plain, predictable JSON.
 *
 * Caching: all three reports share the "reports" cache. Each key starts with the method name
 * (#root.methodName) so different reports never collide. The whole cache is cleared when an
 * order is placed or a product changes (see OrderServiceImpl / ProductServiceImpl), because
 * those are the only events that change report results.
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final int MAX_TOP_PRODUCTS = 100;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;

    public ReportServiceImpl(OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             InventoryRepository inventoryRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /** Defaults to the last 12 months. Both dates are inclusive. */
    @Override
    @Cacheable(cacheNames = CacheNames.REPORTS, key = "#root.methodName + ':' + #from + ':' + #to")
    public List<MonthlyRevenueResponse> getMonthlyRevenue(LocalDate from, LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusMonths(12);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        // The query uses order_date < :to, so pass the start of the NEXT day to include all of 'end'
        return orderRepository.findMonthlyRevenue(start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream()
                .map(row -> new MonthlyRevenueResponse(row.getMonth(), row.getOrderCount(), row.getRevenue()))
                .toList();
    }

    /** limit is clamped to 1..100 so a huge value can't produce a huge response. */
    @Override
    @Cacheable(cacheNames = CacheNames.REPORTS, key = "#root.methodName + ':' + #limit")
    public List<TopSellingProductResponse> getTopSellingProducts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_TOP_PRODUCTS));
        return orderItemRepository.findTopSellingProducts(safeLimit)
                .stream()
                .map(row -> new TopSellingProductResponse(
                        row.getProductId(), row.getProductName(), row.getUnitsSold(), row.getRevenue()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.REPORTS, key = "#root.methodName")
    public List<LowStockResponse> getLowStockProducts() {
        return inventoryRepository.findLowStock()
                .stream()
                .map(inv -> new LowStockResponse(
                        inv.getProductId(), inv.getProduct().getName(), inv.getQuantityOnHand(), inv.getReorderLevel()))
                .toList();
    }
}