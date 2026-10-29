package com.example.Smart_E_Commerce_System.dto.user;

import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.validation.EmailRules;
import com.example.Smart_E_Commerce_System.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for updating a user")
public record UserUpdateRequest(

        @Schema(example = "Jane Doe")
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Schema(example = "jane.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(regexp = EmailRules.REGEX, message = "Email must be a valid address (e.g. name@example.com)")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Schema(description = "Optional; only changed when provided",
                example = "NewSecret456", accessMode = Schema.AccessMode.WRITE_ONLY)
        @StrongPassword
        String password,

        @Schema(example = "ADMIN")
        @NotNull(message = "Role is required")
        Role role
) {}
