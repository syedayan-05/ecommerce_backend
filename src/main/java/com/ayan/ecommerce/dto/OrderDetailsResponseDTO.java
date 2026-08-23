package com.ayan.ecommerce.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailsResponseDTO {

    private Long orderId;

    private String orderNumber;

    private LocalDateTime orderDate;

    private Double amount;

    private String status;

    private String paymentStatus;

    private String paymentMethod;

    private Long addressId;

    private List<OrderItemResponseDTO> items;
}