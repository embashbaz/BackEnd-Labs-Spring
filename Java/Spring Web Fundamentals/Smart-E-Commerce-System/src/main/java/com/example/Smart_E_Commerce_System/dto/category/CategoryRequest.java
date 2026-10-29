package com.example.Smart_E_Commerce_System.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating or updating a category")
public record CategoryRequest(

        @Schema(description = "Unique category name (case-insensitive)", example = "Electronics")
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @Schema(description = "Optional description", example = "Phones, laptops and accessories")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {}
