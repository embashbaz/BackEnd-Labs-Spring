package com.example.Smart_E_Commerce_System.repository;


import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // For updates: is the email taken by a *different* user?
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    Page<User> findByRole(Role role, Pageable pageable);
}
