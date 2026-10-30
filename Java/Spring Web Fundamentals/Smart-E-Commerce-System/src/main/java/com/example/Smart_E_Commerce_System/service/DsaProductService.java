package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;

/** Product operations using hand-written algorithms instead of the database. */
public interface DsaProductService {

    PageResponse<ProductResponse> sortProducts(String sortBy, boolean descending, int page, int size);

    ProductResponse findById(Integer id);
}
