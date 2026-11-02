package com.example.Smart_E_Commerce_System.repository.projection;

import java.math.BigDecimal;

/** Read-only view of one row of the top-selling products native query (matched by column alias). */
public interface TopSellingProduct {
    Integer getProductId();
    String getProductName();
    Long getUnitsSold();
    BigDecimal getRevenue();
}