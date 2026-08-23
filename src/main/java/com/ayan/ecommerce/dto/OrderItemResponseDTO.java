package com.ayan.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDTO {

    private Long productId;

    private String productName;

    private String productImage;

    private Integer quantity;

    private Double price;
}