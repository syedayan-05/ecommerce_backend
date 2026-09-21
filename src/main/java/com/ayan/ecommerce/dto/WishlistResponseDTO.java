package com.ayan.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponseDTO {
    private Long wishlistId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
}
