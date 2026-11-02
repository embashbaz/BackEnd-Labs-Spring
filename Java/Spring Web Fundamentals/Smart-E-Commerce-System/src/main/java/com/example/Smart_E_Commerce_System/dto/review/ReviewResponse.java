package com.example.Smart_E_Commerce_System.dto.review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Integer reviewId,
        Integer productId,
        Integer userId,
        String userName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}