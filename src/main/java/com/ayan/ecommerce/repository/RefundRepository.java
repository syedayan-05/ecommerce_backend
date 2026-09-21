package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.Payment;
import com.ayan.ecommerce.entity.Refund;
import com.ayan.ecommerce.entity.RefundStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository
        extends JpaRepository<Refund, Long> {


    Optional<Refund> findByPayment(
            Payment payment
    );

    Optional<Refund> findByRazorpayRefundId(
            String razorpayRefundId
    );

    Optional<Refund> findByPayment_TransactionId(
            String transactionId
    );

// =========================================================
// ATOMIC REFUND STATUS TRANSITION
// =========================================================
//
// PENDING -> PROCESSED
//
// Only one concurrent request can successfully change the
// status. This prevents duplicate stock restoration.
//

    @Modifying
    @Transactional
    @Query("""
        UPDATE Refund r
        SET r.refundStatus = :newStatus
        WHERE r.id = :refundId
        AND r.refundStatus = :expectedStatus
        """)
    int updateStatusIfCurrentStatus(
            @Param("refundId") Long refundId,
            @Param("expectedStatus") RefundStatus expectedStatus,
            @Param("newStatus") RefundStatus newStatus
    );


}
