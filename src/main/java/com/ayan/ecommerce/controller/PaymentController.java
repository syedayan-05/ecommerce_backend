package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.PaymentRequestDTO;
import com.ayan.ecommerce.dto.PaymentResponseDTO;
import com.ayan.ecommerce.dto.PaymentVerificationDTO;
import com.ayan.ecommerce.entity.Payment;
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

    @PostMapping("/cod")
    public ResponseEntity<?> processCOD(
            @RequestBody PaymentRequestDTO request) {

        PaymentResponseDTO payment =
                paymentService.processCODPayment(
                        request.getOrderId());

        return ResponseEntity.ok(payment);
    }

    @PostMapping("/create-razorpay-order")
    public ResponseEntity<?> createRazorpayOrder(
            @RequestBody PaymentRequestDTO request) {

        try {

            String razorpayOrderId =
                    paymentService.createRazorpayOrder(
                            request.getOrderId());

            return ResponseEntity.ok(
                    Map.of(
                            "razorpayOrderId",
                            razorpayOrderId
                    ));

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }

    }

    @PostMapping("/verify-razorpay")
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerificationDTO dto) {

        boolean success =
                paymentService.verifyAndSavePayment(dto);

        if (success) {

            return ResponseEntity.ok(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "Payment Verified Successfully"
                    ));

        }

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "status", "FAILED",
                        "message", "Invalid Signature"
                ));
    }

    @PutMapping("/cod/{orderId}/complete")
    public ResponseEntity<?> completeCODPayment(
            @PathVariable Long orderId) {

        PaymentResponseDTO response =
                paymentService.completeCODPayment(orderId);

        return ResponseEntity.ok(response);
    }

}