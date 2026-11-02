package com.example.Smart_E_Commerce_System.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of POST /api/products/{productId}/reviews. */
public record CreateReviewRequest(
        @NotNull(message = "User ID is required")
        Integer userId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        String comment
) {}