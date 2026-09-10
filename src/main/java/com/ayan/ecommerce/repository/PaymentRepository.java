package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(
            OrderRequest order
    );

    Optional<Payment> findByTransactionId(
            String transactionId
    );

    Optional<Payment> findByRazorpayOrderId(
            String razorpayOrderId
    );
}