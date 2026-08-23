package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRequestRepository extends JpaRepository<OrderRequest,Long> {
    List<OrderRequest> findByUser(User user);
}
