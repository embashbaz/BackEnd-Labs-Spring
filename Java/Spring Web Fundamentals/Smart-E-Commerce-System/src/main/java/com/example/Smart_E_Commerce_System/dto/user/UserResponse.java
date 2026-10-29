package com.example.Smart_E_Commerce_System.dto.user;


import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.entity.User;

import java.time.LocalDateTime;

/** Never exposes the password hash. */
public record UserResponse(
        Integer id,
        String fullName,
        String email,
        Role role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
