package com.ayan.ecommerce.dto;

import lombok.Data;

@Data
public class PaymentVerificationDTO {
    private Long orderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
