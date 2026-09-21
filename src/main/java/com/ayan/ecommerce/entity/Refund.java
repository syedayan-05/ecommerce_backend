package com.ayan.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "refunds",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_refund_razorpay_id",
                        columnNames = "razorpay_refund_id"
                ),

                @UniqueConstraint(
                        name = "uk_refund_idempotency_key",
                        columnNames = "idempotency_key"
                ),

                @UniqueConstraint(
                        name = "uk_refund_payment_id",
                        columnNames = "payment_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // RAZORPAY REFUND ID
    // Example: rfnd_xxxxxxxxx
    // =========================================================

    @Column(
            name = "razorpay_refund_id",
            unique = true
    )
    private String razorpayRefundId;


    // =========================================================
    // REFUND AMOUNT
    // =========================================================

    @Column(
            nullable = false
    )
    private BigDecimal amount;


    // =========================================================
    // REFUND STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "refund_status",
            nullable = false
    )
    private RefundStatus refundStatus;


    // =========================================================
    // IDEMPOTENCY KEY
    // =========================================================

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;


    // =========================================================
    // PAYMENT
    // =========================================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            unique = true
    )
    private Payment payment;


    // =========================================================
    // CREATED AT
    // =========================================================

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    // =========================================================
    // UPDATED AT
    // =========================================================

    @Column(
            nullable = false
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // BEFORE INSERT
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }


    // =========================================================
    // BEFORE UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}