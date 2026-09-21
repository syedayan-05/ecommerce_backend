package com.ayan.ecommerce.service;

import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.PaymentRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import com.ayan.ecommerce.repository.RefundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RefundTransactionService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRequestRepository orderRepository;
    private final ProductRepository productRepository;

    // PREPARE REFUND

    @Transactional
    public RefundService.RefundPreparation prepareRefund(
            Long orderId
    ) {

        if (orderId == null) {
            throw new RuntimeException(
                    "Order ID is required"
            );
        }

        Payment payment =
                paymentRepository
                        .findByOrderIdForUpdate(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for this order"
                                )
                        );

        OrderRequest order =
                payment.getOrder();

        if (order == null) {
            throw new RuntimeException(
                    "Order not found for payment"
            );
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException(
                    "Only confirmed orders can be refunded"
            );
        }

        if (order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Only completed payments can be refunded"
            );
        }

        if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Payment is not eligible for refund"
            );
        }

        if (payment.getTransactionId() == null ||
                payment.getTransactionId().isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID is missing"
            );
        }

        if (payment.getAmount() == null ||
                payment.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Invalid payment amount"
            );
        }

        Refund existingRefund =
                refundRepository
                        .findByPayment(payment)
                        .orElse(null);

        if (existingRefund != null) {

            if (existingRefund.getRefundStatus() ==
                    RefundStatus.PENDING) {

                return new RefundService.RefundPreparation(
                        payment.getId(),
                        order.getId(),
                        order.getOrderNumber(),
                        payment.getTransactionId(),
                        payment.getAmount(),
                        existingRefund.getIdempotencyKey()
                );
            }

            if (existingRefund.getRefundStatus() ==
                    RefundStatus.PROCESSED) {

                throw new RuntimeException(
                        "Order has already been refunded"
                );
            }

            if (existingRefund.getRefundStatus() ==
                    RefundStatus.FAILED) {

                throw new RuntimeException(
                        "Previous refund failed. Please contact support."
                );
            }
        }

        String idempotencyKey =
                "refund-payment-" + payment.getId();

        Refund refund =
                Refund.builder()
                        .razorpayRefundId(null)
                        .amount(payment.getAmount())
                        .refundStatus(RefundStatus.PENDING)
                        .idempotencyKey(idempotencyKey)
                        .payment(payment)
                        .build();

        refundRepository.save(refund);

        payment.setPaymentStatus(
                PaymentStatus.REFUND_PENDING
        );

        paymentRepository.save(payment);

        return new RefundService.RefundPreparation(
                payment.getId(),
                order.getId(),
                order.getOrderNumber(),
                payment.getTransactionId(),
                payment.getAmount(),
                idempotencyKey
        );
    }

    // SAVE RAZORPAY REFUND ID

    @Transactional
    public void finalizeRefundRequest(
            Long paymentId,
            String razorpayRefundId
    ) {

        if (paymentId == null) {
            throw new RuntimeException(
                    "Payment ID is required"
            );
        }

        if (razorpayRefundId == null ||
                razorpayRefundId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay refund ID is required"
            );
        }

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found"
                                )
                        );

        Refund refund =
                refundRepository
                        .findByPayment(payment)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local refund not found"
                                )
                        );

        if (refund.getRazorpayRefundId() == null) {

            refund.setRazorpayRefundId(
                    razorpayRefundId
            );

            refundRepository.save(refund);

        } else if (!razorpayRefundId.equals(
                refund.getRazorpayRefundId()
        )) {

            throw new RuntimeException(
                    "Razorpay refund ID mismatch"
            );
        }

        if (payment.getPaymentStatus() !=
                PaymentStatus.REFUNDED) {

            payment.setPaymentStatus(
                    PaymentStatus.REFUND_PENDING
            );

            paymentRepository.save(payment);
        }
    }

    // REFUND PROCESSED

    @Transactional
    public void processRefundProcessed(
            String razorpayRefundId,
            String razorpayPaymentId,
            long amountInPaise
    ) {

        if (razorpayRefundId == null ||
                razorpayRefundId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay refund ID is missing"
            );
        }

        if (razorpayPaymentId == null ||
                razorpayPaymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID is missing"
            );
        }

        if (amountInPaise <= 0) {
            throw new RuntimeException(
                    "Invalid refund amount"
            );
        }

        Payment payment =
                paymentRepository
                        .findByTransactionIdForUpdate(
                                razorpayPaymentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for refund"
                                )
                        );

        Refund refund =
                refundRepository
                        .findByRazorpayRefundId(
                                razorpayRefundId
                        )
                        .orElse(
                                refundRepository
                                        .findByPayment(payment)
                                        .orElse(null)
                        );

        if (refund == null) {

            BigDecimal amount =
                    BigDecimal.valueOf(amountInPaise)
                            .movePointLeft(2)
                            .setScale(
                                    2,
                                    RoundingMode.UNNECESSARY
                            );

            refund =
                    Refund.builder()
                            .razorpayRefundId(
                                    razorpayRefundId
                            )
                            .amount(amount)
                            .refundStatus(
                                    RefundStatus.PENDING
                            )
                            .idempotencyKey(
                                    "refund-payment-" +
                                            payment.getId()
                            )
                            .payment(payment)
                            .build();

            refundRepository.save(refund);
        }

        if (refund.getRazorpayRefundId() == null) {

            refund.setRazorpayRefundId(
                    razorpayRefundId
            );

            refundRepository.save(refund);

        } else if (!razorpayRefundId.equals(
                refund.getRazorpayRefundId()
        )) {

            throw new RuntimeException(
                    "Razorpay refund ID mismatch"
            );
        }

        if (refund.getAmount() == null ||
                refund.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Invalid local refund amount"
            );
        }

        long localAmountInPaise;

        try {

            localAmountInPaise =
                    refund.getAmount()
                            .movePointRight(2)
                            .setScale(
                                    0,
                                    RoundingMode.UNNECESSARY
                            )
                            .longValueExact();

        } catch (ArithmeticException e) {

            throw new RuntimeException(
                    "Invalid refund amount precision",
                    e
            );
        }

        if (localAmountInPaise != amountInPaise) {

            throw new RuntimeException(
                    "Refund amount mismatch"
            );
        }

        int updatedRows =
                refundRepository
                        .updateStatusIfCurrentStatus(
                                refund.getId(),
                                RefundStatus.PENDING,
                                RefundStatus.PROCESSED
                        );

        if (updatedRows == 0) {

            Refund currentRefund =
                    refundRepository
                            .findById(refund.getId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Refund not found"
                                    )
                            );

            if (currentRefund.getRefundStatus() ==
                    RefundStatus.PROCESSED) {

                return;
            }

            if (currentRefund.getRefundStatus() ==
                    RefundStatus.FAILED) {

                throw new RuntimeException(
                        "Refund was already marked failed"
                );
            }

            throw new RuntimeException(
                    "Refund could not be moved to PROCESSED"
            );
        }

        payment.setPaymentStatus(
                PaymentStatus.REFUNDED
        );

        paymentRepository.save(payment);

        OrderRequest order =
                payment.getOrder();

        if (order == null) {
            throw new RuntimeException(
                    "Order not found for payment"
            );
        }

        if (order.getStatus() ==
                OrderStatus.CANCELLED) {

            return;
        }

        if (order.getStatus() !=
                OrderStatus.CONFIRMED) {

            throw new RuntimeException(
                    "Only confirmed orders can be cancelled after refund"
            );
        }

        order.setPaymentStatus(
                PaymentStatus.REFUNDED
        );

        if (order.getItems() == null ||
                order.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Order has no items"
            );
        }

        for (OrderItem item : order.getItems()) {

            if (item == null) {
                throw new RuntimeException(
                        "Invalid order item"
                );
            }

            Product product =
                    item.getProduct();

            if (product == null) {
                throw new RuntimeException(
                        "Product not found for order item"
                );
            }

            if (item.getQuantity() == null ||
                    item.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid order item quantity"
                );
            }

            int restoredRows =
                    productRepository.restoreStock(
                            product.getId(),
                            item.getQuantity()
                    );

            if (restoredRows == 0) {

                throw new RuntimeException(
                        "Failed to restore stock for product: "
                                + product.getId()
                );
            }
        }

        order.setStatus(
                OrderStatus.CANCELLED
        );

        orderRepository.save(order);
    }
}