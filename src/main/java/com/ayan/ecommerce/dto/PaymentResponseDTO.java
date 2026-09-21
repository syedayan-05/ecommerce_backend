package com.ayan.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private Long paymentId;

    private String transactionId;

    private BigDecimal amount;

    private String paymentMethod;

    private String paymentStatus;

    private LocalDateTime paymentDate;

    private Long orderId;
}