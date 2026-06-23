package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository
                                extends JpaRepository<Category,Long> {
}
