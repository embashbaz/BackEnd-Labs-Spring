package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.Inventory;
import com.example.Smart_E_Commerce_System.entity.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Query("select i from Inventory i where i.quantityOnHand <= i.reorderLevel")
    List<Inventory> findLowStock();

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryNameIgnoreCase(String categoryName, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByPriceBetween(BigDecimal min, BigDecimal max, Pageable pageable);
}
