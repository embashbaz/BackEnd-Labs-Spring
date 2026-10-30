package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.algorithm.Paging;
import com.example.Smart_E_Commerce_System.algorithm.Searching;
import com.example.Smart_E_Commerce_System.algorithm.Sorting;
import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.service.DsaProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DsaProductServiceImpl implements DsaProductService {

    private final ProductRepository productRepository;

    public DsaProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Merge sort, then manual pagination. */
    @Override
    public PageResponse<ProductResponse> sortProducts(String sortBy, boolean descending, int page, int size) {
        Comparator<ProductResponse> cmp = switch (sortBy) {
            case "name" -> Comparator.comparing(ProductResponse::name, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparing(ProductResponse::price);
            case "id" -> Comparator.comparing(ProductResponse::id);
            default -> throw new IllegalArgumentException("sortBy must be id, name or price");
        };
        if (descending) {
            cmp = cmp.reversed();
        }
        List<ProductResponse> sorted = Sorting.mergeSort(loadAll(), cmp);
        return Paging.paginate(sorted, page, size);
    }

    /** Sort by id, then binary search for the id. */
    @Override
    public ProductResponse findById(Integer id) {
        List<ProductResponse> byId = Sorting.mergeSort(loadAll(), Comparator.comparing(ProductResponse::id));
        int index = Searching.binarySearch(byId, id, ProductResponse::id);
        if (index < 0) {
            throw new ResourceNotFoundException("Product", id);
        }
        return byId.get(index);
    }

    private List<ProductResponse> loadAll() {
        return productRepository.findAllWithCategory().stream().map(ProductResponse::from).toList();
    }
}
