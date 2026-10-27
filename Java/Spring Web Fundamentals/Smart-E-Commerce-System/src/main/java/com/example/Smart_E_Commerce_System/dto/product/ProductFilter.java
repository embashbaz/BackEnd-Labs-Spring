package com.example.Smart_E_Commerce_System.dto.product;

import java.math.BigDecimal;

/** Optional filters for product search; any field may be null. */
public record ProductFilter(
        Integer categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String name
) {}
