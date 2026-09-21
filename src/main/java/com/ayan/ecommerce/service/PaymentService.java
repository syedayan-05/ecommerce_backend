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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

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


    @Transactional
    public String createRazorpayOrder(Long orderId)
            throws RazorpayException {

        if (orderId == null) {
            throw new RuntimeException("Order ID is required");
        }

        User currentUser =
                currentUserService.getCurrentUser();

        if (currentUser == null) {
            throw new RuntimeException("Current user not found");
        }

        OrderRequest order =
                orderRequestRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        if (order.getUser() == null ||
                !Objects.equals(
                        order.getUser().getId(),
                        currentUser.getId()
                )) {

            throw new RuntimeException(
                    "You are not allowed to pay for this order"
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Order is not pending"
            );
        }

        if (order.getPaymentStatus() !=
                PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Payment is not pending"
            );
        }

        if (order.getRazorpayOrderId() != null &&
                !order.getRazorpayOrderId().isBlank()) {

            return order.getRazorpayOrderId();
        }

        BigDecimal amount = order.getAmount();

        if (amount == null ||
                amount.signum() <= 0) {

            throw new RuntimeException(
                    "Invalid order amount"
            );
        }

        long amountInPaise = toPaise(amount);

        RazorpayClient razorpay =
                new RazorpayClient(
                        razorpayKeyId,
                        razorpayKeySecret
                );

        JSONObject request =
                new JSONObject();

        request.put("amount", amountInPaise);
        request.put("currency", "INR");
        request.put(
                "receipt",
                "order_rcpt_" + orderId
        );

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

        order.setRazorpayOrderId(
                razorpayOrderId
        );

        orderRequestRepository.save(order);

        return razorpayOrderId;
    }


    @Transactional
    public PaymentResponseDTO verifyAndSavePayment(
            PaymentVerificationDTO dto
    ) {

        try {

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

            User currentUser =
                    currentUserService.getCurrentUser();

            if (currentUser == null) {
                throw new RuntimeException(
                        "Current user not found"
                );
            }

            OrderRequest order =
                    orderRequestRepository
                            .findById(dto.getOrderId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Order not found"
                                    )
                            );

            if (order.getUser() == null ||
                    !Objects.equals(
                            order.getUser().getId(),
                            currentUser.getId()
                    )) {

                throw new RuntimeException(
                        "You are not allowed to verify payment for this order"
                );
            }

            Payment existingPayment =
                    paymentRepository
                            .findByOrder(order)
                            .orElse(null);

            if (existingPayment != null) {

                if (!Objects.equals(
                        existingPayment.getTransactionId(),
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

            if (order.getStatus() !=
                    OrderStatus.PENDING) {

                throw new RuntimeException(
                        "Order is not pending"
                );
            }

            if (order.getPaymentStatus() !=
                    PaymentStatus.PENDING) {

                throw new RuntimeException(
                        "Payment is not pending"
                );
            }

            String storedRazorpayOrderId =
                    order.getRazorpayOrderId();

            if (storedRazorpayOrderId == null ||
                    storedRazorpayOrderId.isBlank()) {

                throw new RuntimeException(
                        "Razorpay order is not created"
                );
            }

            if (!storedRazorpayOrderId.equals(
                    dto.getRazorpayOrderId()
            )) {

                throw new RuntimeException(
                        "Razorpay order ID mismatch"
                );
            }

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

            RazorpayClient razorpay =
                    new RazorpayClient(
                            razorpayKeyId,
                            razorpayKeySecret
                    );

            com.razorpay.Payment razorpayPayment =
                    razorpay.payments.fetch(
                            dto.getRazorpayPaymentId()
                    );

            String razorpayPaymentId =
                    razorpayPayment.get("id");

            if (!dto.getRazorpayPaymentId()
                    .equals(razorpayPaymentId)) {

                throw new RuntimeException(
                        "Razorpay payment ID mismatch"
                );
            }

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

            long razorpayAmount =
                    ((Number)
                            razorpayPayment.get("amount")
                    ).longValue();

            long expectedAmount =
                    toPaise(order.getAmount());

            if (razorpayAmount != expectedAmount) {

                throw new RuntimeException(
                        "Payment amount mismatch"
                );
            }

            String currency =
                    razorpayPayment.get("currency");

            if (!"INR".equalsIgnoreCase(currency)) {

                throw new RuntimeException(
                        "Invalid payment currency"
                );
            }

            String razorpayStatus =
                    razorpayPayment.get("status");

            if (!"captured".equalsIgnoreCase(
                    razorpayStatus
            )) {

                throw new RuntimeException(
                        "Payment is not captured"
                );
            }

            String razorpayMethod =
                    razorpayPayment.get("method");

            PaymentMethod paymentMethod =
                    mapPaymentMethod(
                            razorpayMethod
                    );

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

            orderService.confirmPaidOrder(
                    order,
                    paymentMethod
            );

            emailService.sendOrderConfirmation(
                    order.getUser().getEmail(),
                    order.getUser().getName(),
                    order.getOrderNumber(),
                    order.getAmount()
            );

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


    private PaymentResponseDTO buildPaymentResponse(
            Payment payment,
            OrderRequest order
    ) {

        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .transactionId(
                        payment.getTransactionId()
                )
                .amount(payment.getAmount())
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
                .orderId(order.getId())
                .build();
    }


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


    private long toPaise(BigDecimal amount) {

        if (amount == null ||
                amount.signum() <= 0) {

            throw new RuntimeException(
                    "Invalid amount"
            );
        }

        try {

            return amount
                    .setScale(
                            2,
                            RoundingMode.UNNECESSARY
                    )
                    .movePointRight(2)
                    .longValueExact();

        } catch (ArithmeticException e) {

            throw new RuntimeException(
                    "Amount must have at most 2 decimal places",
                    e
            );
        }
    }
}