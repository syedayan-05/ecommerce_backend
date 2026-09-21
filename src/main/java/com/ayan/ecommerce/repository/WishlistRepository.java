package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository
        extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserAndProduct(
            User user,
            Product product
    );

    @EntityGraph(attributePaths = "product")
    List<Wishlist> findByUser(User user);

    boolean existsByUserAndProduct(
            User user,
            Product product
    );

    void deleteByUserAndProduct(
            User user,
            Product product
    );
}