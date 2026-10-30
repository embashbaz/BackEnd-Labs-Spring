package com.example.Smart_E_Commerce_System.algorithm;

import com.example.Smart_E_Commerce_System.dto.PageResponse;

import java.util.List;

/** Offset pagination of an in-memory list: items [page * size, page * size + size). */
public final class Paging {

    private Paging() {}

    public static <T> PageResponse<T> paginate(List<T> items, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size must be >= 1");
        }
        int total = items.size();
        int totalPages = (total + size - 1) / size; // ceiling division
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new PageResponse<>(
                List.copyOf(items.subList(from, to)),
                page, size, total, totalPages,
                page >= totalPages - 1);
    }
}
