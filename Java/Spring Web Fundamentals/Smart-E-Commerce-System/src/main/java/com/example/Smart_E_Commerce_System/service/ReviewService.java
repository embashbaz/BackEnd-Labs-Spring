package com.example.Smart_E_Commerce_System.service;

import com.example.Smart_E_Commerce_System.dto.PageResponse; // ADJUST: your PageResponse package
import com.example.Smart_E_Commerce_System.dto.review.RatingSummary; // ADJUST: where you put RatingSummary
import com.example.Smart_E_Commerce_System.dto.review.CreateReviewRequest;
import com.example.Smart_E_Commerce_System.dto.review.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Integer productId, CreateReviewRequest request);

    PageResponse<ReviewResponse> getReviews(Integer productId, Pageable pageable);

    RatingSummary getRatingSummary(Integer productId);
}