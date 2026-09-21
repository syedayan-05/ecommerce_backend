package com.ayan.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_transaction_id",
                        columnNames = "transaction_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_order_id",
                        columnNames = "order_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_razorpay_order_id",
                        columnNames = "razorpay_order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Razorpay Payment ID
    // Example: pay_xxxxxxxxx
    @Column(
            name = "transaction_id",
            nullable = false,
            unique = true
    )
    private String transactionId;

    // Razorpay Order ID
    // Example: order_xxxxxxxxx
    @Column(
            name = "razorpay_order_id",
            nullable = false,
            unique = true
    )
    private String razorpayOrderId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false
    )
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            nullable = false
    )
    private PaymentStatus paymentStatus;

    @Column(
            name = "amount",
            nullable = false
    )
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private OrderRequest order;

    @OneToOne(mappedBy = "payment",fetch = FetchType.LAZY)
    private Refund refund;

    @PrePersist
    protected void onCreate() {

        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

}