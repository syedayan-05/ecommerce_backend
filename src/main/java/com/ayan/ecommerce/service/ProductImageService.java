package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.ProductImageDTO;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.ProductImage;
import com.ayan.ecommerce.repository.ProductImageRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductImageFileValidator productImageFileValidator;

    private final Path uploadDirectory =
            Paths.get("uploads/products")
                    .toAbsolutePath()
                    .normalize();

    // =========================================================
    // UPLOAD PRODUCT IMAGE
    // =========================================================

    @Transactional
    public ProductImageDTO uploadImage(
            Long productId,
            MultipartFile file,
            boolean primary,
            Integer sortOrder
    ) {

        // 1. Check product
        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found"
                                )
                        );

        // 2. Validate sort order
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException(
                    "Sort order must be zero or greater"
            );
        }

        // 3. Validate uploaded file
        productImageFileValidator.validate(file);

        // 4. Get original filename
        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid image filename"
            );
        }

        // 5. Extract extension
        String extension =
                extractExtension(originalFileName);

        // 6. Generate safe UUID filename
        String fileName =
                UUID.randomUUID()
                        + "."
                        + extension;

        // 7. Create upload directory
        try {

            Files.createDirectories(uploadDirectory);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to create upload directory",
                    e
            );
        }

        // 8. Resolve target path safely
        Path targetPath =
                uploadDirectory
                        .resolve(fileName)
                        .normalize();

        // 9. Path traversal protection
        if (!targetPath.startsWith(uploadDirectory)) {

            throw new IllegalArgumentException(
                    "Invalid file path"
            );
        }

        // 10. Save file
        try {

            Files.copy(
                    file.getInputStream(),
                    targetPath
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to save product image",
                    e
            );
        }

        try {

            // 11. If this image is primary,
            // remove primary flag from existing images
            if (primary) {

                List<ProductImage> existingImages =
                        imageRepository.findByProduct(product);

                existingImages.forEach(image ->
                        image.setPrimary(false)
                );

                imageRepository.saveAll(existingImages);
            }

            // 12. Create entity
            ProductImage image =
                    ProductImage.builder()
                            .product(product)
                            .imageUrl(
                                    "/uploads/products/"
                                            + fileName
                            )
                            .primary(primary)
                            .sortOrder(sortOrder)
                            .build();

            // 13. Save entity
            ProductImage savedImage =
                    imageRepository.save(image);

            // 14. Convert entity to DTO
            return mapToDTO(savedImage);

        } catch (RuntimeException e) {

            // DB failure -> remove uploaded file
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException cleanupException) {
                // Keep original exception
            }

            throw e;
        }
    }

    // =========================================================
    // GET PRODUCT IMAGES
    // =========================================================

    @Transactional(readOnly = true)
    public List<ProductImageDTO> getProductImages(
            Long productId
    ) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found"
                                )
                        );

        return imageRepository
                .findByProductOrderBySortOrderAsc(product)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // =========================================================
    // DELETE PRODUCT IMAGE
    // =========================================================

    @Transactional
    public void deleteImage(Long imageId) {

        ProductImage image =
                imageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product image not found"
                                )
                        );

        String imageUrl =
                image.getImageUrl();

        if (imageUrl == null
                || imageUrl.isBlank()) {

            imageRepository.delete(image);
            return;
        }

        // Convert URL to filename
        String fileName =
                Paths.get(imageUrl)
                        .getFileName()
                        .toString();

        Path targetPath =
                uploadDirectory
                        .resolve(fileName)
                        .normalize();

        // Path traversal protection
        if (!targetPath.startsWith(uploadDirectory)) {

            throw new IllegalArgumentException(
                    "Invalid image path"
            );
        }

        // Delete database record
        imageRepository.delete(image);

        // Delete physical file
        try {

            Files.deleteIfExists(targetPath);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Image record deleted but physical file could not be deleted",
                    e
            );
        }
    }

    // =========================================================
    // EXTRACT FILE EXTENSION
    // =========================================================

    private String extractExtension(
            String filename
    ) {

        int lastDot =
                filename.lastIndexOf('.');

        if (lastDot <= 0
                || lastDot == filename.length() - 1) {

            throw new IllegalArgumentException(
                    "Invalid image extension"
            );
        }

        return filename
                .substring(lastDot + 1)
                .toLowerCase();
    }

    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    private ProductImageDTO mapToDTO(
            ProductImage image
    ) {

        ProductImageDTO dto =
                new ProductImageDTO();

        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setPrimary(image.isPrimary());
        dto.setSortOrder(image.getSortOrder());

        return dto;
    }
}