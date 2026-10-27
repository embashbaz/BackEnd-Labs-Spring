package com.example.Smart_E_Commerce_System.repository;


import com.example.Smart_E_Commerce_System.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>,
        JpaSpecificationExecutor<Product> {

    /**
     * Fetches category in the same query (JOIN) to avoid the N+1 problem
     * when mapping a page of products to responses.
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Integer id);

    // Used before deleting a category (FK is ON DELETE RESTRICT)
    boolean existsByCategoryId(Integer categoryId);
}
