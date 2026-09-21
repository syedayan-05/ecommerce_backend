package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<String> razorpayWebhook(

            @RequestBody String rawBody,

            @RequestHeader(
                    name = "X-Razorpay-Signature",
                    required = false
            )
            String signature,

            @RequestHeader(
                    name = "x-razorpay-event-id",
                    required = false
            )
            String eventId
    ) {

        if (rawBody == null || rawBody.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Empty webhook body");
        }

        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Missing Razorpay signature");
        }

        if (eventId == null || eventId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Missing Razorpay event ID");
        }

        webhookService.processWebhook(
                eventId,
                signature,
                rawBody
        );

        return ResponseEntity.ok("OK");
    }
}