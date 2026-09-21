package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.WishlistResponseDTO;
import com.ayan.ecommerce.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponseDTO> addToWishlist(
            @PathVariable Long productId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        wishlistService.addToWishlist(productId)
                );
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> removeFromWishlist(
            @PathVariable Long productId
    ) {

        wishlistService.removeFromWishlist(productId);

        return ResponseEntity.ok(
                "Product removed from wishlist"
        );
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponseDTO>> getMyWishlist() {

        return ResponseEntity.ok(
                wishlistService.getMyWishlist()
        );
    }
}