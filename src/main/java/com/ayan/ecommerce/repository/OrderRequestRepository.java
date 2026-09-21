package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRequestRepository
        extends JpaRepository<OrderRequest, Long> {


    List<OrderRequest> findByUser(User user);

// =========================================================
// RAZORPAY ORDER LOOKUP
// =========================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderRequest> findByRazorpayOrderId(
            String razorpayOrderId
    );

// =========================================================
// ORDER LOCK
// =========================================================
//
// Used during refund completion.
// LEFT JOIN FETCH loads order items in the same query.
//

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT DISTINCT o
        FROM OrderRequest o
        LEFT JOIN FETCH o.items
        WHERE o.id = :orderId
        """)
    Optional<OrderRequest> findByIdForUpdate(
            @Param("orderId") Long orderId
    );


}
