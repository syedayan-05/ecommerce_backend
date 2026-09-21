package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.WishlistResponseDTO;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.entity.Wishlist;
import com.ayan.ecommerce.repository.ProductRepository;
import com.ayan.ecommerce.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    @Transactional
    public WishlistResponseDTO addToWishlist(Long productId) {

        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }

        User user = userService.getLoggedInUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            throw new RuntimeException(
                    "Product is already in your wishlist"
            );
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist savedWishlist =
                wishlistRepository.save(wishlist);

        return mapToResponse(savedWishlist);
    }

    @Transactional
    public void removeFromWishlist(Long productId) {

        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }

        User user = userService.getLoggedInUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        Wishlist wishlist =
                wishlistRepository.findByUserAndProduct(
                        user,
                        product
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Product is not in your wishlist"
                        )
                );

        wishlistRepository.delete(wishlist);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponseDTO> getMyWishlist() {

        User user = userService.getLoggedInUser();

        return wishlistRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private WishlistResponseDTO mapToResponse(
            Wishlist wishlist
    ) {

        Product product = wishlist.getProduct();

        return WishlistResponseDTO.builder()
                .wishlistId(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .build();
    }
}