package com.ayan.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDTO {

    private Long id;

    private String imageUrl;

    private boolean primary;

    private Integer sortOrder;
}