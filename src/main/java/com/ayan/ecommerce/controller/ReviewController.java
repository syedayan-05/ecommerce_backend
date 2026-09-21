package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.ReviewRequestDTO;
import com.ayan.ecommerce.dto.ReviewResponseDTO;
import com.ayan.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;


    // =========================================================
    // ADD REVIEW
    // =========================================================

    @PostMapping("/{productId}")
    public ResponseEntity<ReviewResponseDTO> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequestDTO dto
    ) {

        ReviewResponseDTO response =
                reviewService.addReview(
                        productId,
                        dto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // GET PRODUCT REVIEWS
    // =========================================================

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getProductReviews(
            @PathVariable Long productId
    ) {

        return ResponseEntity.ok(
                reviewService.getProductReviews(
                        productId
                )
        );
    }


    // =========================================================
    // UPDATE REVIEW
    // =========================================================

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequestDTO dto
    ) {

        return ResponseEntity.ok(
                reviewService.updateReview(
                        reviewId,
                        dto
                )
        );
    }


    // =========================================================
    // DELETE REVIEW
    // =========================================================

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId
    ) {

        reviewService.deleteReview(reviewId);

        return ResponseEntity.noContent().build();
    }
}