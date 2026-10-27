package com.example.Smart_E_Commerce_System.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable JSON shape for paginated results (serializing Spring's Page directly
 * is discouraged because its structure isn't guaranteed).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
