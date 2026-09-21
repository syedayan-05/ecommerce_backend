package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.Gallery;
import com.ayan.ecommerce.entity.GalleryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryRepository extends JpaRepository<Gallery, Long> {

    List<Gallery> findByActiveTrueOrderBySortOrderAsc();

    List<Gallery> findByCategoryAndActiveTrueOrderBySortOrderAsc(
            GalleryCategory category
    );
}