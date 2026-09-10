package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRequestRepository
        extends JpaRepository<OrderRequest, Long> {

    List<OrderRequest> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderRequest> findByRazorpayOrderId(
            String razorpayOrderId
    );
}