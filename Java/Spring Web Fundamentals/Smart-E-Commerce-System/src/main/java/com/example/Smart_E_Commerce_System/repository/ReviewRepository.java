package com.example.Smart_E_Commerce_System.repository;

import com.example.Smart_E_Commerce_System.dto.review.RatingSummary;
import com.example.Smart_E_Commerce_System.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    @EntityGraph(attributePaths = "user")
    Page<Review> findByProductId(Integer productId, Pageable pageable);

    boolean existsByProductIdAndUserId(Integer productId, Integer userId);

    /** Average rating and review count; empty Optional if the product has no reviews. */
    @Query("""
            select new com.example.Smart_E_Commerce_System.dto.review.RatingSummary(
                r.product.id, avg(r.rating), count(r))
            from Review r
            where r.product.id = :productId
            group by r.product.id
            """)
    Optional<RatingSummary> findRatingSummary(@Param("productId") Integer productId);
}
