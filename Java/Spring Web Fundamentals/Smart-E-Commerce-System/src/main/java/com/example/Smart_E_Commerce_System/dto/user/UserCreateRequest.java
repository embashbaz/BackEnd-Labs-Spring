package com.example.Smart_E_Commerce_System.dto.user;


import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.validation.StrongPassword;
import com.example.Smart_E_Commerce_System.validation.UniqueEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @UniqueEmail
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        // Optional, defaults to CUSTOMER
        Role role
) {}
