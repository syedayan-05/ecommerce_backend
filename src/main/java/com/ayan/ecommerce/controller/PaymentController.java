package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.PaymentRequestDTO;
import com.ayan.ecommerce.dto.PaymentResponseDTO;
import com.ayan.ecommerce.dto.PaymentVerificationDTO;
import com.ayan.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;


    // =========================================================
    // CREATE RAZORPAY ORDER
    // =========================================================

    @PostMapping("/create-razorpay-order")
    public ResponseEntity<?> createRazorpayOrder(
            @RequestBody PaymentRequestDTO request
    ) {

        try {

            String razorpayOrderId =
                    paymentService.createRazorpayOrder(
                            request.getOrderId()
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "razorpayOrderId",
                            razorpayOrderId
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // VERIFY RAZORPAY CHECKOUT PAYMENT
    // =========================================================

    @PostMapping("/verify-razorpay")
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerificationDTO dto
    ) {

        PaymentResponseDTO payment =
                paymentService.verifyAndSavePayment(
                        dto
                );

        return ResponseEntity.ok(payment);
    }
}