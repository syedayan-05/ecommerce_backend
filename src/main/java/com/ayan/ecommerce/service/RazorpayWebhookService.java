package com.ayan.ecommerce.service;

import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.PaymentRepository;
import com.ayan.ecommerce.repository.RazorpayWebhookEventRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final RazorpayWebhookEventRepository webhookEventRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final  EmailService emailService;

    @Value("${razorpay.webhook_secret}")
    private String webhookSecret;


// =========================================================
// HANDLE WEBHOOK
// =========================================================

    @Transactional
    public void handleWebhook(
            String rawBody,
            String signature,
            String eventId
    ) throws RazorpayException {

        // -----------------------------------------------------
        // BASIC VALIDATION
        // -----------------------------------------------------

        if (rawBody == null || rawBody.isBlank()) {
            throw new RuntimeException(
                    "Webhook body is empty"
            );
        }

        if (signature == null || signature.isBlank()) {
            throw new RuntimeException(
                    "Webhook signature missing"
            );
        }

        if (eventId == null || eventId.isBlank()) {
            throw new RuntimeException(
                    "Webhook event ID missing"
            );
        }


        // -----------------------------------------------------
        // VERIFY RAZORPAY WEBHOOK SIGNATURE
        // -----------------------------------------------------

        boolean valid =
                Utils.verifyWebhookSignature(
                        rawBody,
                        signature,
                        webhookSecret
                );

        if (!valid) {
            throw new RuntimeException(
                    "Invalid Razorpay webhook signature"
            );
        }


        // -----------------------------------------------------
        // DUPLICATE EVENT CHECK
        // -----------------------------------------------------

        if (webhookEventRepository
                .findByEventId(eventId)
                .isPresent()) {

            System.out.println(
                    "Duplicate Razorpay webhook ignored: "
                            + eventId
            );

            return;
        }


        // -----------------------------------------------------
        // PARSE PAYLOAD
        // -----------------------------------------------------

        JSONObject payload =
                new JSONObject(rawBody);

        String eventType =
                payload.getString("event");


        // -----------------------------------------------------
        // SAVE WEBHOOK EVENT
        // -----------------------------------------------------

        RazorpayWebhookEvent event =
                RazorpayWebhookEvent.builder()
                        .eventId(eventId)
                        .eventType(eventType)
                        .processingStatus(
                                WebhookProcessingStatus.PROCESSED
                        )
                        .build();

        webhookEventRepository.save(event);


        // -----------------------------------------------------
        // HANDLE EVENTS
        // -----------------------------------------------------

        switch (eventType) {

            case "payment.captured":
                handlePaymentCaptured(payload);
                break;

            case "order.paid":
                handleOrderPaid(payload);
                break;

            case "payment.failed":
                handlePaymentFailed(payload);
                break;

            default:
                System.out.println(
                        "Unhandled Razorpay webhook event: "
                                + eventType
                );
                break;
        }
    }


// =========================================================
// PAYMENT CAPTURED
// =========================================================

    private void handlePaymentCaptured(
            JSONObject payload
    ) {

        JSONObject paymentEntity =
                payload
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        synchronizeSuccessfulPayment(
                paymentEntity
        );
    }


// =========================================================
// ORDER PAID
// =========================================================

    private void handleOrderPaid(
            JSONObject payload
    ) {

        System.out.println(
                "Razorpay order.paid received. "
                        + "Payment processing is handled by "
                        + "payment.captured."
        );
    }


// =========================================================
// PAYMENT FAILED
// =========================================================

    private void handlePaymentFailed(
            JSONObject payload
    ) {

        JSONObject paymentEntity =
                payload
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        String razorpayPaymentId =
                paymentEntity.getString("id");

        String razorpayOrderId =
                paymentEntity.optString(
                        "order_id",
                        null
                );

        if (razorpayOrderId == null) {
            return;
        }

        OrderRequest order =
                orderRequestRepository
                        .findByRazorpayOrderId(
                                razorpayOrderId
                        )
                        .orElse(null);

        if (order == null) {
            return;
        }

        /*
         * Do not cancel the order here.
         *
         * Customer may retry payment.
         */

        System.out.println(
                "Razorpay payment failed: "
                        + razorpayPaymentId
        );
    }


// =========================================================
// CENTRAL PAYMENT SYNCHRONIZATION
// =========================================================

    private void synchronizeSuccessfulPayment(
            JSONObject paymentEntity
    ) {

        String razorpayPaymentId =
                paymentEntity.getString("id");

        String razorpayOrderId =
                paymentEntity.getString("order_id");

        String status =
                paymentEntity.getString("status");

        long amountInPaise =
                paymentEntity.getLong("amount");

        String method =
                paymentEntity.getString("method");


        // -----------------------------------------------------
        // ONLY CAPTURED PAYMENT
        // -----------------------------------------------------

        if (!"captured".equalsIgnoreCase(status)) {
            return;
        }


        // -----------------------------------------------------
        // FIND OUR ORDER
        // -----------------------------------------------------

        OrderRequest order =
                orderRequestRepository
                        .findByRazorpayOrderId(
                                razorpayOrderId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local order not found for Razorpay order: "
                                                + razorpayOrderId
                                )
                        );


        // -----------------------------------------------------
        // AMOUNT VALIDATION
        // -----------------------------------------------------

        long expectedAmount =
                Math.round(
                        order.getAmount() * 100
                );

        if (amountInPaise != expectedAmount) {

            throw new RuntimeException(
                    "Webhook payment amount mismatch"
            );
        }


        // -----------------------------------------------------
        // IDEMPOTENCY - PAYMENT ID
        // -----------------------------------------------------

        if (paymentRepository
                .findByTransactionId(
                        razorpayPaymentId
                )
                .isPresent()) {

            System.out.println(
                    "Payment already processed: "
                            + razorpayPaymentId
            );

            return;
        }


        // -----------------------------------------------------
        // IDEMPOTENCY - ORDER
        // -----------------------------------------------------

        if (order.getPaymentStatus() ==
                PaymentStatus.COMPLETED) {

            System.out.println(
                    "Order payment already completed: "
                            + razorpayOrderId
            );

            return;
        }


        // -----------------------------------------------------
        // MAP PAYMENT METHOD
        // -----------------------------------------------------

        PaymentMethod paymentMethod =
                mapPaymentMethod(method);


        // -----------------------------------------------------
        // SAVE PAYMENT
        // -----------------------------------------------------

        Payment payment =
                Payment.builder()
                        .order(order)
                        .razorpayOrderId(
                                razorpayOrderId
                        )
                        .transactionId(
                                razorpayPaymentId
                        )
                        .amount(
                                order.getAmount()
                        )
                        .paymentMethod(
                                paymentMethod
                        )
                        .paymentStatus(
                                PaymentStatus.COMPLETED
                        )
                        .build();

        paymentRepository.save(payment);


        // -----------------------------------------------------
        // CONFIRM ORDER
        // -----------------------------------------------------

        orderService.confirmPaidOrder(
                order,
                paymentMethod
        );

        //send order confirmation email
        System.out.println("========order email start=========");
        System.out.println(
                "Email : "  + order.getUser().getEmail()
        );
        System.out.println(
                "Customer : " + order.getUser().getName()
        );
        System.out.println(
                "Order Number : " + order.getOrderNumber()
        );
        System.out.println(
                "Amount: " + order.getAmount()
        );
        emailService.sendOrderConfirmation(
                order.getUser().getEmail(),
                order.getUser().getName(),
                order.getOrderNumber(),
                order.getAmount()
        );
        System.out.println("========== ORDER EMAIL END ==========");


        System.out.println(
                "Payment successfully synchronized: "
                        + razorpayPaymentId
        );
    }


// =========================================================
// PAYMENT METHOD MAPPING
// =========================================================

    private PaymentMethod mapPaymentMethod(
            String method
    ) {

        return switch (
                method.toLowerCase()
                ) {

            case "upi" ->
                    PaymentMethod.UPI;

            case "card" ->
                    PaymentMethod.CARD;

            case "netbanking" ->
                    PaymentMethod.NET_BANKING;

            default ->
                    throw new RuntimeException(
                            "Unsupported Razorpay payment method: "
                                    + method
                    );
        };
    }


}
