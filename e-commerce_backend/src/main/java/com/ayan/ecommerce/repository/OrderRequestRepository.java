package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OrderRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRequestRepository extends JpaRepository<OrderRequest,Long> {}
