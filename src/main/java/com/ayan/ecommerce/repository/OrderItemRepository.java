package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderItem;
import com.ayan.ecommerce.entity.OrderStatus;
import com.ayan.ecommerce.entity.PaymentStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    @Query("""
            SELECT CASE
                WHEN COUNT(oi) > 0 THEN true
                ELSE false
            END
            FROM OrderItem oi
            JOIN oi.orderRequest o
            WHERE o.user.id = :userId
            AND oi.product.id = :productId
            AND o.paymentStatus = :paymentStatus
            AND o.status = :orderStatus
            """)
    boolean existsPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("orderStatus") OrderStatus orderStatus
    );
}