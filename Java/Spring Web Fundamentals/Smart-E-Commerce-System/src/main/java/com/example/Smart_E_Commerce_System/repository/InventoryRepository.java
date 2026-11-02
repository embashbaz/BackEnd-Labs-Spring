package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.Inventory;
import com.example.Smart_E_Commerce_System.entity.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Modifying
    @Query("""
            update Inventory i
            set i.quantityOnHand = i.quantityOnHand - :qty,
                i.updatedAt = local datetime
            where i.productId = :productId
              and i.quantityOnHand >= :qty
            """)
    int decrementStock(@Param("productId") Integer productId, @Param("qty") int qty);

    /** Products at or below their reorder level (compares two columns, so JPQL, not derived). */
    @Query("""
            select i from Inventory i
            join fetch i.product
            where i.quantityOnHand <= i.reorderLevel
            order by i.quantityOnHand
            """)
    List<Inventory> findLowStock();

}
