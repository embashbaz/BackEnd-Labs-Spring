package com.example.Smart_E_Commerce_System.dto.review;

/** Average rating and number of reviews for one product. */
public record RatingSummary(
        Integer productId,
        Double averageRating,
        Long reviewCount
) {}
