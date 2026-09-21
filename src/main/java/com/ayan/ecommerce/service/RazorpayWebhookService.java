package com.ayan.ecommerce.service;

import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.PaymentRepository;
import com.ayan.ecommerce.repository.RazorpayWebhookEventRepository;
import com.razorpay.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final RazorpayWebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRequestRepository orderRepository;
    private final RefundTransactionService refundTransactionService;
    private final OrderService orderService;
    private final EmailService emailService;

    @Value("${razorpay.webhook_secret}")
    private String webhookSecret;


    // =========================================================
    // MAIN WEBHOOK
    // =========================================================

    @Transactional
    public void processWebhook(
            String eventId,
            String signature,
            String payload
    ) {

        if (eventId == null || eventId.isBlank()) {
            throw new RuntimeException(
                    "Razorpay webhook event ID is missing"
            );
        }

        if (signature == null || signature.isBlank()) {
            throw new RuntimeException(
                    "Razorpay webhook signature is missing"
            );
        }

        if (payload == null || payload.isBlank()) {
            throw new RuntimeException(
                    "Razorpay webhook payload is empty"
            );
        }

        // -----------------------------------------------------
        // 1. VERIFY SIGNATURE FIRST
        // -----------------------------------------------------

        try {

            Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid Razorpay webhook signature",
                    e
            );
        }

        // -----------------------------------------------------
        // 2. IDEMPOTENCY
        // -----------------------------------------------------

        if (webhookEventRepository
                .findByEventId(eventId)
                .isPresent()) {

            return;
        }

        // -----------------------------------------------------
        // 3. PARSE RAW PAYLOAD
        // -----------------------------------------------------

        JSONObject root;

        try {

            root = new JSONObject(payload);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid Razorpay webhook JSON",
                    e
            );
        }

        String eventType =
                root.optString("event", null);

        if (eventType == null || eventType.isBlank()) {

            throw new RuntimeException(
                    "Missing Razorpay event type"
            );
        }

        // -----------------------------------------------------
        // 4. HANDLE EVENT
        // -----------------------------------------------------

        switch (eventType) {

            case "payment.captured" ->
                    handlePaymentCaptured(root);

            case "payment.failed" ->
                    handlePaymentFailed(root);

            case "refund.created" ->
                    handleRefundCreated(root);

            case "refund.processed" ->
                    handleRefundProcessed(root);

            case "refund.failed" ->
                    handleRefundFailed(root);

            default -> {
                // Event received but not currently handled.
            }
        }

        // -----------------------------------------------------
        // 5. SAVE EVENT
        // -----------------------------------------------------

        webhookEventRepository.save(
                RazorpayWebhookEvent.builder()
                        .eventId(eventId)
                        .eventType(eventType)
                        .build()
        );
    }


    // =========================================================
    // PAYMENT CAPTURED
    // =========================================================

    private void handlePaymentCaptured(
            JSONObject root
    ) {

        JSONObject paymentEntity =
                root.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        String razorpayPaymentId =
                paymentEntity.optString("id", null);

        String razorpayOrderId =
                paymentEntity.optString("order_id", null);

        long amountInPaise =
                paymentEntity.optLong("amount", 0);

        String currency =
                paymentEntity.optString("currency", null);

        String razorpayStatus =
                paymentEntity.optString("status", null);

        String razorpayMethod =
                paymentEntity.optString("method", null);


        if (razorpayPaymentId == null ||
                razorpayPaymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID missing"
            );
        }

        if (razorpayOrderId == null ||
                razorpayOrderId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay order ID missing"
            );
        }

        if (amountInPaise <= 0) {

            throw new RuntimeException(
                    "Invalid payment amount"
            );
        }

        if (!"INR".equalsIgnoreCase(currency)) {

            throw new RuntimeException(
                    "Invalid payment currency"
            );
        }

        if (!"captured".equalsIgnoreCase(
                razorpayStatus
        )) {

            throw new RuntimeException(
                    "Payment is not captured"
            );
        }


        // -----------------------------------------------------
        // FIND LOCAL ORDER USING RAZORPAY ORDER ID
        // -----------------------------------------------------

        OrderRequest order =
                orderRepository
                        .findByRazorpayOrderId(
                                razorpayOrderId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local order not found for Razorpay order"
                                )
                        );


        // -----------------------------------------------------
        // VALIDATE AMOUNT
        // -----------------------------------------------------

//        long expectedAmount =
//                Math.round(
//                        order.getAmount() * 100
//                );

        long expectedAmount = order.getAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        if (expectedAmount != amountInPaise) {

            throw new RuntimeException(
                    "Payment amount mismatch"
            );
        }


        // -----------------------------------------------------
        // CHECK EXISTING PAYMENT
        // -----------------------------------------------------

        Payment existingPayment =
                paymentRepository
                        .findByTransactionId(
                                razorpayPaymentId
                        )
                        .orElse(null);

        if (existingPayment != null) {

            // Already successfully processed.
            if (existingPayment.getPaymentStatus() ==
                    PaymentStatus.COMPLETED) {

                return;
            }

            throw new RuntimeException(
                    "Payment already exists in unexpected state"
            );
        }


        // -----------------------------------------------------
        // MAP PAYMENT METHOD
        // -----------------------------------------------------

        PaymentMethod paymentMethod =
                mapPaymentMethod(
                        razorpayMethod
                );


        // -----------------------------------------------------
        // CREATE LOCAL PAYMENT
        // -----------------------------------------------------

        Payment payment =
                Payment.builder()
                        .transactionId(
                                razorpayPaymentId
                        )
                        .razorpayOrderId(
                                razorpayOrderId
                        )
                        .paymentMethod(
                                paymentMethod
                        )
                        .paymentStatus(
                                PaymentStatus.COMPLETED
                        )
                        .amount(
                                order.getAmount()
                        )
                        .order(order)
                        .build();

        paymentRepository.save(payment);


        // -----------------------------------------------------
        // CONFIRM ORDER + STOCK + CART
        // -----------------------------------------------------

        orderService.confirmPaidOrder(
                order,
                paymentMethod
        );


        // -----------------------------------------------------
        // ORDER EMAIL
        // -----------------------------------------------------

        emailService.sendOrderConfirmation(
                order.getUser().getEmail(),
                order.getUser().getName(),
                order.getOrderNumber(),
                order.getAmount()
        );
    }


    // =========================================================
    // PAYMENT FAILED
    // =========================================================

    private void handlePaymentFailed(
            JSONObject root
    ) {

        JSONObject paymentEntity =
                root.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        String razorpayPaymentId =
                paymentEntity.optString("id", null);

        if (razorpayPaymentId == null ||
                razorpayPaymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID missing"
            );
        }

        Payment payment =
                paymentRepository
                        .findByTransactionId(
                                razorpayPaymentId
                        )
                        .orElse(null);

        if (payment == null) {
            return;
        }

        if (payment.getPaymentStatus() ==
                PaymentStatus.PENDING) {

            payment.setPaymentStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            OrderRequest order =
                    payment.getOrder();

            if (order != null &&
                    order.getPaymentStatus() ==
                            PaymentStatus.PENDING) {

                order.setPaymentStatus(
                        PaymentStatus.FAILED
                );

                orderRepository.save(order);
            }
        }
    }


    // =========================================================
    // REFUND CREATED
    // =========================================================

    private void handleRefundCreated(
            JSONObject root
    ) {

        JSONObject refundEntity =
                root.getJSONObject("payload")
                        .getJSONObject("refund")
                        .getJSONObject("entity");

        String refundId =
                refundEntity.optString("id", null);

        String paymentId =
                refundEntity.optString(
                        "payment_id",
                        null
                );

        if (refundId == null ||
                refundId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay refund ID missing"
            );
        }

        if (paymentId == null ||
                paymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID missing"
            );
        }

        // Nothing else here.
        //
        // refund.created != refund completed.
    }


    // =========================================================
    // REFUND PROCESSED
    // =========================================================

    private void handleRefundProcessed(
            JSONObject root
    ) {

        JSONObject refundEntity =
                root.getJSONObject("payload")
                        .getJSONObject("refund")
                        .getJSONObject("entity");

        String refundId =
                refundEntity.optString("id", null);

        String paymentId =
                refundEntity.optString(
                        "payment_id",
                        null
                );

        long amountInPaise =
                refundEntity.optLong(
                        "amount",
                        0
                );

        String status =
                refundEntity.optString(
                        "status",
                        null
                );

        if (refundId == null ||
                refundId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay refund ID missing"
            );
        }

        if (paymentId == null ||
                paymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID missing"
            );
        }

        if (amountInPaise <= 0) {

            throw new RuntimeException(
                    "Invalid refund amount"
            );
        }

        if (status != null &&
                !"processed".equalsIgnoreCase(status)) {

            throw new RuntimeException(
                    "Invalid refund status: " + status
            );
        }

        refundTransactionService
                .processRefundProcessed(
                        refundId,
                        paymentId,
                        amountInPaise
                );
    }


    // =========================================================
    // REFUND FAILED
    // =========================================================

    private void handleRefundFailed(
            JSONObject root
    ) {

        JSONObject refundEntity =
                root.getJSONObject("payload")
                        .getJSONObject("refund")
                        .getJSONObject("entity");

        String paymentId =
                refundEntity.optString(
                        "payment_id",
                        null
                );

        if (paymentId == null ||
                paymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID missing"
            );
        }

        Payment payment =
                paymentRepository
                        .findByTransactionId(
                                paymentId
                        )
                        .orElse(null);

        if (payment == null) {
            return;
        }

        if (payment.getPaymentStatus() ==
                PaymentStatus.REFUND_PENDING) {

            payment.setPaymentStatus(
                    PaymentStatus.COMPLETED
            );

            paymentRepository.save(payment);

            OrderRequest order =
                    payment.getOrder();

            if (order != null &&
                    order.getPaymentStatus() ==
                            PaymentStatus.REFUND_PENDING) {

                order.setPaymentStatus(
                        PaymentStatus.COMPLETED
                );

                order.setStatus(
                        OrderStatus.CONFIRMED
                );

                orderRepository.save(order);
            }
        }
    }


    // =========================================================
    // PAYMENT METHOD MAPPING
    // =========================================================

    private PaymentMethod mapPaymentMethod(
            String razorpayMethod
    ) {

        if (razorpayMethod == null ||
                razorpayMethod.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment method missing"
            );
        }

        return switch (
                razorpayMethod.toLowerCase()
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
                                    + razorpayMethod
                    );
        };
    }
}