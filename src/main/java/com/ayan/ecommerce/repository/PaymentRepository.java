package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<Payment> findByOrderId(
            Long orderId
    );

// =========================================================
// LOCK PAYMENT ROW
// =========================================================
//
// Used during refund preparation / webhook processing.
// Prevents two concurrent refund operations from modifying
// the same payment at the same time.
//

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.order.id = :orderId
        """)
    Optional<Payment> findByOrderIdForUpdate(
            @Param("orderId") Long orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.transactionId = :transactionId
        """)
    Optional<Payment> findByTransactionIdForUpdate(
            @Param("transactionId") String transactionId
    );

}
