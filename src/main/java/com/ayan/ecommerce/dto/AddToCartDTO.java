package com.ayan.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartDTO {

    private Long cartId;

    private Long productId;

    private Integer quantity;
}