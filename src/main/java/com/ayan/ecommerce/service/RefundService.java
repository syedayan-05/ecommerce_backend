package com.ayan.ecommerce.service;

import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundTransactionService refundTransactionService;
    private final CurrentUserService currentUserService;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;


    public String requestFullRefund(OrderRequest order) {

        if (order == null) {
            throw new RuntimeException(
                    "Order is required"
            );
        }

        User currentUser =
                currentUserService.getCurrentUser();

        if (currentUser == null) {
            throw new RuntimeException(
                    "Current user not found"
            );
        }

        if (order.getUser() == null ||
                order.getUser().getId() == null ||
                !order.getUser()
                        .getId()
                        .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You are not allowed to refund this order"
            );
        }

        RefundPreparation preparation =
                refundTransactionService.prepareRefund(
                        order.getId()
                );

        BigDecimal amount =
                preparation.amount();

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Invalid refund amount"
            );
        }

        long amountInPaise;

        try {
            amountInPaise =
                    amount
                            .setScale(
                                    2,
                                    RoundingMode.UNNECESSARY
                            )
                            .movePointRight(2)
                            .longValueExact();

        } catch (ArithmeticException e) {

            throw new RuntimeException(
                    "Refund amount must have maximum 2 decimal places",
                    e
            );
        }

        if (amountInPaise <= 0) {
            throw new RuntimeException(
                    "Invalid refund amount"
            );
        }

        JSONObject refundRequest =
                new JSONObject();

        refundRequest.put(
                "amount",
                amountInPaise
        );

        refundRequest.put(
                "speed",
                "normal"
        );

        refundRequest.put(
                "receipt",
                preparation.idempotencyKey()
        );

        refundRequest.put(
                "notes",
                new JSONObject()
                        .put(
                                "order_id",
                                preparation.orderId()
                        )
                        .put(
                                "order_number",
                                preparation.orderNumber()
                        )
                        .put(
                                "reason",
                                "Customer requested order cancellation"
                        )
        );

        try {

            RestClient restClient =
                    RestClient.builder()
                            .baseUrl(
                                    "https://api.razorpay.com"
                            )
                            .defaultHeaders(headers -> {

                                headers.setBasicAuth(
                                        razorpayKeyId,
                                        razorpayKeySecret
                                );

                                headers.setContentType(
                                        MediaType.APPLICATION_JSON
                                );

                                headers.set(
                                        "X-Refund-Idempotency",
                                        preparation.idempotencyKey()
                                );
                            })
                            .build();

            String response =
                    restClient.post()
                            .uri(
                                    "/v1/payments/{paymentId}/refund",
                                    preparation.paymentTransactionId()
                            )
                            .body(
                                    refundRequest.toString()
                            )
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, response1) -> {

                                        throw new RuntimeException(
                                                "Razorpay refund API failed. "
                                                        + "HTTP status: "
                                                        + response1.getStatusCode()
                                        );
                                    }
                            )
                            .body(String.class);

            if (response == null ||
                    response.isBlank()) {

                throw new RuntimeException(
                        "Empty response received from Razorpay"
                );
            }

            JSONObject refundResponse =
                    new JSONObject(response);

            String razorpayRefundId =
                    refundResponse.optString(
                            "id",
                            null
                    );

            String razorpayStatus =
                    refundResponse.optString(
                            "status",
                            "pending"
                    );

            if (razorpayRefundId == null ||
                    razorpayRefundId.isBlank()) {

                throw new RuntimeException(
                        "Razorpay refund ID was not generated"
                );
            }

            refundTransactionService.finalizeRefundRequest(
                    preparation.paymentId(),
                    razorpayRefundId
            );

            return
                    "Refund requested successfully. "
                            + "Refund ID: "
                            + razorpayRefundId
                            + ". Razorpay status: "
                            + razorpayStatus;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Refund request could not be confirmed. "
                            + "Refund remains pending and can be "
                            + "safely reconciled.",
                    e
            );
        }
    }


    public record RefundPreparation(
            Long paymentId,
            Long orderId,
            String orderNumber,
            String paymentTransactionId,
            BigDecimal amount,
            String idempotencyKey
    ) {
    }
}