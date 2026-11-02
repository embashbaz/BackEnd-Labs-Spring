package com.example.Smart_E_Commerce_System.repository.projection;

import java.math.BigDecimal;

/** Read-only view of one row of the monthly revenue native query (matched by column alias). */
public interface MonthlyRevenue {
    String getMonth();          // "2026-03"
    Long getOrderCount();
    BigDecimal getRevenue();
}