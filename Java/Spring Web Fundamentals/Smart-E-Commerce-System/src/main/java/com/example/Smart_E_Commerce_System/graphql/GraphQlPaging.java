package com.example.Smart_E_Commerce_System.graphql;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Turns GraphQL paging arguments into a Spring Pageable, with the same limits as REST. */
final class GraphQlPaging {

    private static final int MAX_PAGE_SIZE = 100;

    private GraphQlPaging() {}

    static Pageable of(int page, int size, String sortBy, Sort.Direction direction) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(direction, sortBy));
    }
}
