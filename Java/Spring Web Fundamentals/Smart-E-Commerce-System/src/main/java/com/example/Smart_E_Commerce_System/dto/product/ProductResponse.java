package com.example.Smart_E_Commerce_System.dto.product;


import com.example.Smart_E_Commerce_System.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Integer id,
        String name,
        String description,
        BigDecimal price,
        Integer categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
