package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.PaymentResponseDTO;
import com.ayan.ecommerce.dto.PaymentVerificationDTO;
import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {


    private final PaymentRepository paymentRepository;
    private final OrderRequestRepository orderRequestRepository;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;


    // ==========================================
// 1. CASH ON DELIVERY PAYMENT
// ==========================================
    @Transactional
    public PaymentResponseDTO processCODPayment(Long orderId) {

        // Find Order
        OrderRequest order = orderRequestRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        // Prevent duplicate payment
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException(
                    "Payment already exists for this order");
        }

        // Create COD Payment
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getAmount())
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(
                        "COD_" +
                                UUID.randomUUID()
                                        .toString()
                                        .substring(0, 8)
                                        .toUpperCase()
                )
                .build();

        Payment savedPayment =
                paymentRepository.save(payment);

        // Update Order Payment Information
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);

        orderRequestRepository.save(order);

        // Return DTO
        return PaymentResponseDTO.builder()
                .paymentId(savedPayment.getId())
                .transactionId(
                        savedPayment.getTransactionId())
                .amount(savedPayment.getAmount())
                .paymentMethod(savedPayment.getPaymentMethod().name())
                .paymentStatus(
                        savedPayment.getPaymentStatus().name())
                .paymentDate(savedPayment.getPaymentDate())
                .orderId(order.getId())
                .build();
    }


    // ==========================================
// 2. CREATE RAZORPAY ORDER
// ==========================================
    @Transactional(readOnly = true)
    public String createRazorpayOrder(Long orderId)
            throws Exception {

        // Find Order
        OrderRequest order =
                orderRequestRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"));

        // Prevent payment again
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException(
                    "Payment already exists for this order");
        }

        // Check order amount
        if (order.getAmount() == null ||
                order.getAmount() <= 0) {

            throw new RuntimeException(
                    "Invalid order amount");
        }

        RazorpayClient razorpay =
                new RazorpayClient(
                        razorpayKeyId,
                        razorpayKeySecret
                );

        JSONObject request = new JSONObject();

        // Razorpay amount is in paise
        request.put(
                "amount",
                Math.round(order.getAmount() * 100)
        );

        request.put("currency", "INR");

        request.put(
                "receipt",
                "order_rcpt_" + orderId
        );

        Order razorpayOrder =
                razorpay.orders.create(request);

        return razorpayOrder.get("id");
    }


    // ==========================================
// 3. VERIFY RAZORPAY PAYMENT
// ==========================================
    @Transactional
    public boolean verifyAndSavePayment(
            PaymentVerificationDTO dto) {

        // Find Order first
        OrderRequest order =
                orderRequestRepository
                        .findById(dto.getOrderId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"));

        // Prevent duplicate payment verification
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException(
                    "Payment already exists for this order");
        }

        try {

            // Razorpay Signature Verification Data
            JSONObject options = new JSONObject();

            options.put(
                    "razorpay_order_id",
                    dto.getRazorpayOrderId()
            );

            options.put(
                    "razorpay_payment_id",
                    dto.getRazorpayPaymentId()
            );

            options.put(
                    "razorpay_signature",
                    dto.getRazorpaySignature()
            );

            // Verify Razorpay Signature
            boolean isSignatureValid =
                    Utils.verifyPaymentSignature(
                            options,
                            razorpayKeySecret
                    );

            if (!isSignatureValid) {
                return false;
            }

            // Save Payment
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(order.getAmount())

                    // Abhi Razorpay test mein UPI use kar rahe hain
                    .paymentMethod(PaymentMethod.UPI)

                    .paymentStatus(
                            PaymentStatus.COMPLETED
                    )

                    .transactionId(
                            dto.getRazorpayPaymentId()
                    )
                    .build();

            paymentRepository.save(payment);


            // =================================
            // IMPORTANT: UPDATE ORDER
            // =================================

            // Payment successful
            order.setPaymentStatus(
                    PaymentStatus.COMPLETED
            );

            // Payment method
            order.setPaymentMethod(
                    PaymentMethod.UPI
            );

            // Order confirmed
            order.setStatus(
                    OrderStatus.CONFIRMED
            );

            orderRequestRepository.save(order);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Payment verification failed"
            );
        }

    }

    @Transactional
    public PaymentResponseDTO completeCODPayment(Long orderId){
        OrderRequest order = orderRequestRepository.findById(orderId)
                .orElseThrow(()->
                        new RuntimeException("Order Not Found"));
        if (order.getPaymentMethod() != PaymentMethod.COD){
            throw new RuntimeException(
                    "This order is not Cash on Delivery"
            );
        }

        if (order.getStatus() != OrderStatus.DELIVERED){
            throw new RuntimeException(
                    "Order must be delivered before completing Cash on Delivery payment"
            );
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() ->
                        new RuntimeException("Cash on Delivery  payment not found"));

        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Cash on Delivery payment is already completed"
            );
        }

        payment.setPaymentStatus(PaymentStatus.COMPLETED);

        Payment savedPayment =
                paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.COMPLETED);

        orderRequestRepository.save(order);

        return PaymentResponseDTO.builder()
                .paymentId(savedPayment.getId())
                .transactionId(savedPayment.getTransactionId())
                .amount(savedPayment.getAmount())
                .paymentMethod(savedPayment.getPaymentMethod().name())
                .paymentStatus(savedPayment.getPaymentStatus().name())
                .paymentDate(savedPayment.getPaymentDate())
                .orderId(order.getId())
                .build();

    }


}
