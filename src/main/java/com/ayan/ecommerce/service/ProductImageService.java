package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.ProductImageDTO;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.ProductImage;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.ProductImageRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;


    // =========================
    // ENTITY -> DTO
    // =========================

    private ProductImageDTO mapToDTO(ProductImage image) {

        return ProductImageDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .primary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }


    // =========================
    // ADD PRODUCT IMAGE
    // =========================

    @Transactional
    public ProductImageDTO addImage(
            Long productId,
            String imageUrl,
            boolean primary,
            Integer sortOrder) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id " + productId
                        ));

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Image URL is required"
            );
        }

        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException(
                    "Sort order cannot be negative"
            );
        }

        // =========================
        // PRIMARY IMAGE HANDLING
        // =========================

        if (primary) {

            List<ProductImage> existingImages =
                    imageRepository.findByProductId(productId);

            existingImages.forEach(image ->
                    image.setPrimary(false)
            );
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl.trim())
                .primary(primary)
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .build();

        ProductImage savedImage =
                imageRepository.save(image);

        return mapToDTO(savedImage);
    }


    // =========================
    // GET PRODUCT IMAGES
    // =========================

    public List<ProductImageDTO> getProductImages(
            Long productId) {

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(
                    "Product not found with id " + productId
            );
        }

        return imageRepository
                .findByProductIdOrderBySortOrderAsc(productId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }


    // =========================
    // DELETE IMAGE
    // =========================

    @Transactional
    public void deleteImage(Long imageId) {

        ProductImage image =
                imageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product image not found with id "
                                                + imageId
                                ));

        imageRepository.delete(image);
    }
}