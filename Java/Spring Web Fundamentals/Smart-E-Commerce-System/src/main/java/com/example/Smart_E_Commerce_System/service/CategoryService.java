package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.category.CategoryRequest;
import com.example.Smart_E_Commerce_System.dto.category.CategoryResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse getById(Integer id);

    PageResponse<CategoryResponse> getAll(Pageable pageable);

    CategoryResponse update(Integer id, CategoryRequest request);

    void delete(Integer id);
}
