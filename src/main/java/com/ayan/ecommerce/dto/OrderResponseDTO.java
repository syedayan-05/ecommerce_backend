package com.ayan.ecommerce.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {


    private Long orderId;

    private String orderNumber;

    private Double amount;

    private String status;

    private String paymentStatus;

    private String paymentMethod;

    private LocalDateTime orderDate;


}
