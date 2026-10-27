package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    boolean existsByNameIgnoreCase(String name);
}
