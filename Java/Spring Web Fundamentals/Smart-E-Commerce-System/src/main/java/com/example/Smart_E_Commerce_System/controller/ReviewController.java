package com.example.Smart_E_Commerce_System.controller;

import com.example.Smart_E_Commerce_System.dto.ApiResponse;
import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.review.CreateReviewRequest;
import com.example.Smart_E_Commerce_System.dto.review.RatingSummary;
import com.example.Smart_E_Commerce_System.dto.review.ReviewResponse;
import com.example.Smart_E_Commerce_System.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}")
@Tag(name = "Reviews", description = "Product reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    @Operation(summary = "Add a review", description = "One review per user per product; a second one returns 409.")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@PathVariable Integer productId,
                                                                    @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = reviewService.createReview(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success( "Review created successfully", review));
    }

    @GetMapping("/reviews")
    @Operation(summary = "List a product's reviews", description = "Paginated. Sortable by createdAt, rating. Default: newest first.")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @PathVariable Integer productId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        PageResponse<ReviewResponse> page = reviewService.getReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", page));
    }

    @GetMapping("/rating")
    @Operation(summary = "Average rating and review count")
    public ResponseEntity<ApiResponse<RatingSummary>> getRating(@PathVariable Integer productId) {
        RatingSummary summary = reviewService.getRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success( "Rating retrieved successfully", summary));
    }
}