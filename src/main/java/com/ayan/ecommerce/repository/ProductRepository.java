package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByCategoryId(Long categoryId);

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock - :quantity
            WHERE p.id = :productId
            AND p.stock >= :quantity
            """)
    int decrementStockIfAvailable(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock + :quantity
            WHERE p.id = :productId
            """)
    int restoreStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );
}