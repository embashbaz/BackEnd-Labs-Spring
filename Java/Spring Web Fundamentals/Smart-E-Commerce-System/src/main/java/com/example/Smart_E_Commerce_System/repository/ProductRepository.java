package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
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

    /** Every product with its category in one query; used by the in-memory DSA endpoints. */
    @Query("select p from Product p join fetch p.category")
    List<Product> findAllWithCategory();

    // Used before deleting a category (FK is ON DELETE RESTRICT)
    boolean existsByCategoryId(Integer categoryId);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryNameIgnoreCase(String categoryName, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByPriceBetween(BigDecimal min, BigDecimal max, Pageable pageable);
}
