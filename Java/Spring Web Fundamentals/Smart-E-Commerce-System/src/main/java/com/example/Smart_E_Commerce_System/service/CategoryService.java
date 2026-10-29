package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.category.CategoryRequest;
import com.example.Smart_E_Commerce_System.dto.category.CategoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse getById(Integer id);

    PageResponse<CategoryResponse> getAll(Pageable pageable);

    /** Loads many categories in one query; used by GraphQL batch loading. */
    Map<Integer, CategoryResponse> getByIds(Collection<Integer> ids);

    CategoryResponse update(Integer id, CategoryRequest request);

    void delete(Integer id);
}
