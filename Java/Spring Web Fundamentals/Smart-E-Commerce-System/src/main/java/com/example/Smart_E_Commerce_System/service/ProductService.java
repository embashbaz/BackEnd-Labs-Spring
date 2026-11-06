package com.example.Smart_E_Commerce_System.service;


import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductFilter;
import com.example.Smart_E_Commerce_System.dto.product.ProductRequest;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse getById(Integer id);

    PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable);

    ProductResponse update(Integer id, ProductRequest request);

    void delete(Integer id);

    PageResponse<ProductResponse> getByCategoryName(String categoryName, Pageable pageable);

    PageResponse<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable);
}
