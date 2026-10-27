package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JpaSpecificationExecutor enables dynamic filtering
 * (category, price range, name search) combined with paging and sorting.
 */
public interface ProductRepository extends JpaRepository<Product, Integer>,
        JpaSpecificationExecutor<Product> {

    boolean existsByCategoryId(Integer categoryId);
}
