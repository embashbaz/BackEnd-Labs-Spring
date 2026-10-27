package com.example.Smart_E_Commerce_System.dto.category;



import com.example.Smart_E_Commerce_System.entity.Category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Integer id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getCreatedAt());
    }
}
