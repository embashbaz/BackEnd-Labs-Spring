package com.example.Smart_E_Commerce_System.dto.user;


import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.validation.EmailRules;
import com.example.Smart_E_Commerce_System.validation.StrongPassword;
import com.example.Smart_E_Commerce_System.validation.UniqueEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for registering a user")
public record UserCreateRequest(

        @Schema(example = "Jane Doe")
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Schema(description = "Must be unique (case-insensitive)", example = "jane@example.com")
        @NotBlank(message = "Email is required")
        @Email(regexp = EmailRules.REGEX, message = "Email must be a valid address (e.g. name@example.com)")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @UniqueEmail
        String email,

        @Schema(description = "Min 8 chars with uppercase, lowercase and a digit",
                example = "Secret123", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @Schema(description = "Defaults to CUSTOMER when omitted", example = "CUSTOMER")
        Role role
) {}
