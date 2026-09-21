package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.ReviewRequestDTO;
import com.ayan.ecommerce.dto.ReviewResponseDTO;
import com.ayan.ecommerce.entity.OrderStatus;
import com.ayan.ecommerce.entity.PaymentStatus;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.Review;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.OrderItemRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import com.ayan.ecommerce.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;


    // =========================================================
    // ADD REVIEW
    // =========================================================

    @Transactional
    public ReviewResponseDTO addReview(
            Long productId,
            ReviewRequestDTO dto
    ) {

        if (productId == null) {
            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        if (dto == null) {
            throw new RuntimeException(
                    "Review request is required"
            );
        }

        User user =
                userService.getLoggedInUser();

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        // -----------------------------------------------------
        // PURCHASE VERIFICATION
        // -----------------------------------------------------

        boolean purchased =
                orderItemRepository.existsPurchasedProduct(
                        user.getId(),
                        productId,
                        PaymentStatus.COMPLETED,
                        OrderStatus.DELIVERED
                );

        if (!purchased) {

            throw new RuntimeException(
                    "You can review this product only after purchasing and receiving it"
            );
        }


        // -----------------------------------------------------
        // DUPLICATE REVIEW CHECK
        // -----------------------------------------------------

        if (reviewRepository.existsByUserIdAndProductId(
                user.getId(),
                productId
        )) {

            throw new RuntimeException(
                    "You have already reviewed this product"
            );
        }


        // -----------------------------------------------------
        // SAVE REVIEW
        // -----------------------------------------------------

        String comment =
                dto.getComment();

        if (comment != null) {
            comment = comment.trim();

            if (comment.isBlank()) {
                comment = null;
            }
        }

        Review review =
                Review.builder()
                        .user(user)
                        .product(product)
                        .rating(dto.getRating())
                        .comment(comment)
                        .build();

        Review savedReview =
                reviewRepository.save(review);

        return mapToResponse(savedReview);
    }


    // =========================================================
    // GET PRODUCT REVIEWS
    // =========================================================

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getProductReviews(
            Long productId
    ) {

        if (productId == null) {
            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        // Make sure product exists
        if (!productRepository.existsById(productId)) {

            throw new RuntimeException(
                    "Product not found"
            );
        }

        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // UPDATE REVIEW
    // =========================================================

    @Transactional
    public ReviewResponseDTO updateReview(
            Long reviewId,
            ReviewRequestDTO dto
    ) {

        if (reviewId == null) {
            throw new RuntimeException(
                    "Review ID is required"
            );
        }

        if (dto == null) {
            throw new RuntimeException(
                    "Review request is required"
            );
        }

        User currentUser =
                userService.getLoggedInUser();

        Review review =
                reviewRepository.findById(reviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                )
                        );


        // -----------------------------------------------------
        // OWNERSHIP CHECK
        // -----------------------------------------------------

        if (review.getUser() == null ||
                review.getUser().getId() == null ||
                !review.getUser()
                        .getId()
                        .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You are not allowed to update this review"
            );
        }


        // -----------------------------------------------------
        // UPDATE
        // -----------------------------------------------------

        review.setRating(
                dto.getRating()
        );

        String comment =
                dto.getComment();

        if (comment != null) {
            comment = comment.trim();

            if (comment.isBlank()) {
                comment = null;
            }
        }

        review.setComment(comment);

        Review updatedReview =
                reviewRepository.save(review);

        return mapToResponse(updatedReview);
    }


    // =========================================================
    // DELETE REVIEW
    // =========================================================

    @Transactional
    public void deleteReview(
            Long reviewId
    ) {

        if (reviewId == null) {
            throw new RuntimeException(
                    "Review ID is required"
            );
        }

        User currentUser =
                userService.getLoggedInUser();

        Review review =
                reviewRepository.findById(reviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                )
                        );


        // -----------------------------------------------------
        // OWNERSHIP CHECK
        // -----------------------------------------------------

        if (review.getUser() == null ||
                review.getUser().getId() == null ||
                !review.getUser()
                        .getId()
                        .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You are not allowed to delete this review"
            );
        }


        reviewRepository.delete(review);
    }


    // =========================================================
    // MAP ENTITY → RESPONSE DTO
    // =========================================================

    private ReviewResponseDTO mapToResponse(
            Review review
    ) {

        if (review == null) {
            throw new RuntimeException(
                    "Review is required"
            );
        }

        return ReviewResponseDTO.builder()
                .reviewId(review.getId())
                .productId(
                        review.getProduct() != null
                                ? review.getProduct().getId()
                                : null
                )
                .userName(
                        review.getUser() != null
                                ? review.getUser().getName()
                                : null
                )
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}