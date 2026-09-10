package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.PaymentResponseDTO;
import com.ayan.ecommerce.dto.PaymentVerificationDTO;
import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;


    // =========================================================
    // CREATE RAZORPAY ORDER
    // =========================================================

    @Transactional
    public String createRazorpayOrder(Long orderId)
            throws RazorpayException {

        // -----------------------------------------------------
        // 1. VALIDATE ORDER ID
        // -----------------------------------------------------

        if (orderId == null) {

            throw new RuntimeException(
                    "Order ID is required"
            );
        }


        // -----------------------------------------------------
        // 2. GET LOGGED-IN USER
        // -----------------------------------------------------

        User currentUser =
                currentUserService.getCurrentUser();


        // -----------------------------------------------------
        // 3. FIND LOCAL ORDER
        // -----------------------------------------------------

        OrderRequest order =
                orderRequestRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );


        // -----------------------------------------------------
        // 4. SECURITY CHECK
        // -----------------------------------------------------

        if (order.getUser() == null ||
                !order.getUser()
                        .getId()
                        .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You are not allowed to pay for this order"
            );
        }


        // -----------------------------------------------------
        // 5. ORDER STATUS VALIDATION
        // -----------------------------------------------------

        if (order.getStatus() !=
                OrderStatus.PENDING) {

            throw new RuntimeException(
                    "Order is not pending"
            );
        }


        // -----------------------------------------------------
        // 6. PAYMENT STATUS VALIDATION
        // -----------------------------------------------------

        if (order.getPaymentStatus() !=
                PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Payment is not pending"
            );
        }


        // -----------------------------------------------------
        // 7. RETURN EXISTING RAZORPAY ORDER
        // -----------------------------------------------------

        /*
         * Razorpay order already exists.
         *
         * We return the same Razorpay Order ID instead of
         * creating another Razorpay order.
         */

        if (order.getRazorpayOrderId() != null &&
                !order.getRazorpayOrderId().isBlank()) {

            return order.getRazorpayOrderId();
        }


        // -----------------------------------------------------
        // 8. VALIDATE ORDER AMOUNT
        // -----------------------------------------------------

        if (order.getAmount() == null ||
                order.getAmount() <= 0) {

            throw new RuntimeException(
                    "Invalid order amount"
            );
        }


        // -----------------------------------------------------
        // 9. CREATE RAZORPAY CLIENT
        // -----------------------------------------------------

        RazorpayClient razorpay =
                new RazorpayClient(
                        razorpayKeyId,
                        razorpayKeySecret
                );


        // -----------------------------------------------------
        // 10. CONVERT RUPEES → PAISE
        // -----------------------------------------------------

        long amountInPaise =
                Math.round(
                        order.getAmount() * 100
                );


        // -----------------------------------------------------
        // 11. CREATE RAZORPAY REQUEST
        // -----------------------------------------------------

        JSONObject request =
                new JSONObject();

        request.put(
                "amount",
                amountInPaise
        );

        request.put(
                "currency",
                "INR"
        );

        request.put(
                "receipt",
                "order_rcpt_" + orderId
        );


        // -----------------------------------------------------
        // 12. CREATE RAZORPAY ORDER
        // -----------------------------------------------------

        Order razorpayOrder =
                razorpay.orders.create(request);


        String razorpayOrderId =
                razorpayOrder.get("id");


        if (razorpayOrderId == null ||
                razorpayOrderId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay order ID was not generated"
            );
        }


        // -----------------------------------------------------
        // 13. SAVE RAZORPAY ORDER ID
        // -----------------------------------------------------

        order.setRazorpayOrderId(
                razorpayOrderId
        );

        orderRequestRepository.save(order);


        return razorpayOrderId;
    }


    // =========================================================
    // VERIFY RAZORPAY PAYMENT
    // =========================================================

    @Transactional
    public PaymentResponseDTO verifyAndSavePayment(
            PaymentVerificationDTO dto
    ) {

        try {

            // -------------------------------------------------
            // 1. BASIC REQUEST VALIDATION
            // -------------------------------------------------

            if (dto == null) {

                throw new RuntimeException(
                        "Payment verification request is required"
                );
            }

            if (dto.getOrderId() == null) {

                throw new RuntimeException(
                        "Order ID is required"
                );
            }

            if (dto.getRazorpayOrderId() == null ||
                    dto.getRazorpayOrderId().isBlank()) {

                throw new RuntimeException(
                        "Razorpay order ID is required"
                );
            }

            if (dto.getRazorpayPaymentId() == null ||
                    dto.getRazorpayPaymentId().isBlank()) {

                throw new RuntimeException(
                        "Razorpay payment ID is required"
                );
            }

            if (dto.getRazorpaySignature() == null ||
                    dto.getRazorpaySignature().isBlank()) {

                throw new RuntimeException(
                        "Razorpay signature is required"
                );
            }


            // -------------------------------------------------
            // 2. GET LOGGED-IN USER
            // -------------------------------------------------

            User currentUser =
                    currentUserService.getCurrentUser();


            // -------------------------------------------------
            // 3. FIND LOCAL ORDER
            // -------------------------------------------------

            OrderRequest order =
                    orderRequestRepository
                            .findById(dto.getOrderId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Order not found"
                                    )
                            );


            // -------------------------------------------------
            // 4. SECURITY CHECK
            // -------------------------------------------------

            if (order.getUser() == null ||
                    !order.getUser()
                            .getId()
                            .equals(currentUser.getId())) {

                throw new RuntimeException(
                        "You are not allowed to verify payment for this order"
                );
            }


            // -------------------------------------------------
            // 5. IDEMPOTENCY CHECK
            // -----------------------------------------------------

            /*
             * Webhook and frontend verification can sometimes
             * reach the backend very close to each other.
             *
             * If payment has already been successfully saved
             * for this order, return that existing payment.
             */

            Payment existingPayment =
                    paymentRepository
                            .findByOrder(order)
                            .orElse(null);

            if (existingPayment != null) {

                if (!existingPayment
                        .getTransactionId()
                        .equals(
                                dto.getRazorpayPaymentId()
                        )) {

                    throw new RuntimeException(
                            "A different payment already exists for this order"
                    );
                }

                return buildPaymentResponse(
                        existingPayment,
                        order
                );
            }


            // -------------------------------------------------
            // 6. ORDER STATUS VALIDATION
            // -------------------------------------------------

            if (order.getStatus() !=
                    OrderStatus.PENDING) {

                throw new RuntimeException(
                        "Order is not pending"
                );
            }


            // -------------------------------------------------
            // 7. PAYMENT STATUS VALIDATION
            // -------------------------------------------------

            if (order.getPaymentStatus() !=
                    PaymentStatus.PENDING) {

                throw new RuntimeException(
                        "Payment is not pending"
                );
            }


            // -------------------------------------------------
            // 8. GET STORED RAZORPAY ORDER ID
            // -------------------------------------------------

            String storedRazorpayOrderId =
                    order.getRazorpayOrderId();


            if (storedRazorpayOrderId == null ||
                    storedRazorpayOrderId.isBlank()) {

                throw new RuntimeException(
                        "Razorpay order is not created"
                );
            }


            // -------------------------------------------------
            // 9. VALIDATE RAZORPAY ORDER ID
            // -------------------------------------------------

            if (!storedRazorpayOrderId.equals(
                    dto.getRazorpayOrderId()
            )) {

                throw new RuntimeException(
                        "Razorpay order ID mismatch"
                );
            }


            // -------------------------------------------------
            // 10. VERIFY RAZORPAY SIGNATURE
            // -------------------------------------------------

            JSONObject options =
                    new JSONObject();

            options.put(
                    "razorpay_order_id",
                    storedRazorpayOrderId
            );

            options.put(
                    "razorpay_payment_id",
                    dto.getRazorpayPaymentId()
            );

            options.put(
                    "razorpay_signature",
                    dto.getRazorpaySignature()
            );


            boolean valid =
                    Utils.verifyPaymentSignature(
                            options,
                            razorpayKeySecret
                    );


            if (!valid) {

                throw new RuntimeException(
                        "Invalid Razorpay payment signature"
                );
            }


            // -------------------------------------------------
            // 11. FETCH PAYMENT DIRECTLY FROM RAZORPAY
            // -------------------------------------------------

            RazorpayClient razorpay =
                    new RazorpayClient(
                            razorpayKeyId,
                            razorpayKeySecret
                    );


            com.razorpay.Payment razorpayPayment =
                    razorpay.payments.fetch(
                            dto.getRazorpayPaymentId()
                    );


            // -------------------------------------------------
            // 12. VERIFY PAYMENT ID
            // -------------------------------------------------

            String razorpayPaymentId =
                    razorpayPayment.get("id");


            if (!dto.getRazorpayPaymentId()
                    .equals(razorpayPaymentId)) {

                throw new RuntimeException(
                        "Razorpay payment ID mismatch"
                );
            }


            // -------------------------------------------------
            // 13. VERIFY PAYMENT BELONGS TO OUR ORDER
            // -------------------------------------------------

            String razorpayOrderId =
                    razorpayPayment.get("order_id");


            if (razorpayOrderId == null ||
                    !storedRazorpayOrderId.equals(
                            razorpayOrderId
                    )) {

                throw new RuntimeException(
                        "Payment does not belong to this order"
                );
            }


            // -------------------------------------------------
            // 14. VERIFY PAYMENT AMOUNT
            // -------------------------------------------------

            long razorpayAmount =
                    ((Number)
                            razorpayPayment.get("amount")
                    ).longValue();


            long expectedAmount =
                    Math.round(
                            order.getAmount() * 100
                    );


            if (razorpayAmount != expectedAmount) {

                throw new RuntimeException(
                        "Payment amount mismatch"
                );
            }


            // -------------------------------------------------
            // 15. VERIFY CURRENCY
            // -------------------------------------------------

            String currency =
                    razorpayPayment.get("currency");


            if (!"INR".equalsIgnoreCase(currency)) {

                throw new RuntimeException(
                        "Invalid payment currency"
                );
            }


            // -------------------------------------------------
            // 16. VERIFY PAYMENT STATUS
            // -------------------------------------------------

            String razorpayStatus =
                    razorpayPayment.get("status");


            if (!"captured".equalsIgnoreCase(
                    razorpayStatus
            )) {

                throw new RuntimeException(
                        "Payment is not captured"
                );
            }


            // -------------------------------------------------
            // 17. GET ACTUAL PAYMENT METHOD
            // -------------------------------------------------

            String razorpayMethod =
                    razorpayPayment.get("method");


            PaymentMethod paymentMethod =
                    mapPaymentMethod(
                            razorpayMethod
                    );


            // -------------------------------------------------
            // 18. SAVE PAYMENT
            // -------------------------------------------------

            Payment payment =
                    Payment.builder()
                            .order(order)
                            .razorpayOrderId(
                                    storedRazorpayOrderId
                            )
                            .transactionId(
                                    dto.getRazorpayPaymentId()
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


            Payment savedPayment =
                    paymentRepository.save(payment);


            // -------------------------------------------------
            // 19. CONFIRM ORDER
            // -------------------------------------------------

            orderService.confirmPaidOrder(
                    order,
                    paymentMethod
            );


//            send order conformation email

            emailService.sendOrderConfirmation(
                    order.getUser().getEmail(),
                    order.getUser().getName(),
                    order.getOrderNumber(),
                    order.getAmount()
            );


            // -------------------------------------------------
            // 20. RETURN RESPONSE
            // -------------------------------------------------

            return buildPaymentResponse(
                    savedPayment,
                    order
            );


        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Razorpay communication failed: "
                            + e.getMessage(),
                    e
            );

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Payment verification failed: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // BUILD PAYMENT RESPONSE
    // =========================================================

    private PaymentResponseDTO buildPaymentResponse(
            Payment payment,
            OrderRequest order
    ) {

        return PaymentResponseDTO.builder()
                .paymentId(
                        payment.getId()
                )
                .transactionId(
                        payment.getTransactionId()
                )
                .amount(
                        payment.getAmount()
                )
                .paymentMethod(
                        payment.getPaymentMethod() != null
                                ? payment.getPaymentMethod().name()
                                : null
                )
                .paymentStatus(
                        payment.getPaymentStatus() != null
                                ? payment.getPaymentStatus().name()
                                : null
                )
                .paymentDate(
                        payment.getPaymentDate()
                )
                .orderId(
                        order.getId()
                )
                .build();
    }


    // =========================================================
    // MAP RAZORPAY PAYMENT METHOD → OUR ENUM
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

