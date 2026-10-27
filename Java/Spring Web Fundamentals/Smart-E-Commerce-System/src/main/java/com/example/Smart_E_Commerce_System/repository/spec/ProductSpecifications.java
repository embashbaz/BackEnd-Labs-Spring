package com.example.Smart_E_Commerce_System.repository.spec;


import com.example.Smart_E_Commerce_System.dto.product.ProductFilter;
import com.example.Smart_E_Commerce_System.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic WHERE clause from whichever filters were supplied.
 * Filtering happens in the database, so only matching rows are transferred.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> fromFilter(ProductFilter f) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (f.categoryId() != null) specs.add(hasCategory(f.categoryId()));
        if (f.minPrice() != null)   specs.add(priceAtLeast(f.minPrice()));
        if (f.maxPrice() != null)   specs.add(priceAtMost(f.maxPrice()));
        if (f.name() != null && !f.name().isBlank()) specs.add(nameContains(f.name()));
        return Specification.allOf(specs);
    }

    public static Specification<Product> hasCategory(Integer categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> nameContains(String text) {
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }
}
