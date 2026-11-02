package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.dto.PageResponse; // ADJUST: where you put RatingSummary
import com.example.Smart_E_Commerce_System.dto.review.CreateReviewRequest;
import com.example.Smart_E_Commerce_System.dto.review.RatingSummary;
import com.example.Smart_E_Commerce_System.dto.review.ReviewResponse;
import com.example.Smart_E_Commerce_System.entity.Product;
import com.example.Smart_E_Commerce_System.entity.Review;
import com.example.Smart_E_Commerce_System.entity.User;
import com.example.Smart_E_Commerce_System.exception.DuplicateResourceException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.repository.ReviewRepository;
import com.example.Smart_E_Commerce_System.repository.UserRepository;
import com.example.Smart_E_Commerce_System.service.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Integer productId, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product",productId));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        // Friendly 409 for the normal case. The unique constraint (product_id, user_id)
        // in the database still protects against two simultaneous requests.
        if (reviewRepository.existsByProductIdAndUserId(productId, request.userId())) {
            throw new DuplicateResourceException("The user has already reviewed this product");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating().shortValue()); // column is SMALLINT
        review.setComment(request.comment());

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(Integer productId, Pageable pageable) {
        requireProduct(productId);
        return PageResponse.from(reviewRepository.findByProductId(productId, pageable).map(this::toResponse));
    }

    /** Average rounded to 2 decimals. No reviews -> average null, count 0. */
    @Override
    @Transactional(readOnly = true)
    public RatingSummary getRatingSummary(Integer productId) {
        requireProduct(productId);
        return reviewRepository.findRatingSummary(productId)
                .map(s -> new RatingSummary(s.productId(), Math.round(s.averageRating() * 100.0) / 100.0, s.reviewCount()))
                .orElse(new RatingSummary(productId, null, 0L));
    }

    private void requireProduct(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating().intValue(),
                review.getComment(),
                review.getCreatedAt());
    }
}