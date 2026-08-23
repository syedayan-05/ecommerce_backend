package com.ayan.ecommerce.dto;

import com.ayan.ecommerce.dto.CartItemResponseDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDTO {

    private Long cartId;

    private List<CartItemResponseDTO> items;

    private BigDecimal total;
}