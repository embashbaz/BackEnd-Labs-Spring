package com.example.Smart_E_Commerce_System.dto.user;

import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        // Optional: only changed when provided
        @StrongPassword
        String password,

        @NotNull(message = "Role is required")
        Role role
) {}
