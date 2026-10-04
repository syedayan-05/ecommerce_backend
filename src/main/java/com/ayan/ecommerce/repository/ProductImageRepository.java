package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    List<ProductImage> findByProductId(Long productId);

    List<ProductImage> findByProductOrderBySortOrderAsc(Product product);

    List<ProductImage> findByProduct(
            Product product
    );
}