package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.service.RazorpayWebhookService;
import com.razorpay.RazorpayException;
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
    ) throws RazorpayException {

        System.out.println("======================================");
        System.out.println("RAZORPAY WEBHOOK HIT");
        System.out.println("EVENT ID: " + eventId);
        System.out.println("SIGNATURE: " + signature);
        System.out.println("BODY: " + rawBody);
        System.out.println("======================================");

        webhookService.handleWebhook(
                rawBody,
                signature,
                eventId
        );

        return ResponseEntity.ok("OK");
    }
}