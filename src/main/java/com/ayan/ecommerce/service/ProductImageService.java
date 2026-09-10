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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;

    private final Path uploadDirectory =
            Paths.get("uploads/products").toAbsolutePath().normalize();


    // =========================
    // CONSTANTS
    // =========================

    private static final long MAX_FILE_SIZE =
            5 * 1024 * 1024; // 5 MB


    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );


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
    // UPLOAD PRODUCT IMAGE
    // =========================

    @Transactional
    public ProductImageDTO uploadImage(
            Long productId,
            MultipartFile file,
            boolean primary,
            Integer sortOrder) {

        // =========================
        // PRODUCT CHECK
        // =========================

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id " + productId
                        ));


        // =========================
        // FILE CHECK
        // =========================

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }


        // =========================
        // FILE SIZE CHECK
        // =========================

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "Image size cannot exceed 5 MB"
            );
        }


        // =========================
        // SORT ORDER CHECK
        // =========================

        if (sortOrder == null || sortOrder < 0) {

            throw new IllegalArgumentException(
                    "Sort order must be zero or greater"
            );
        }


        // =========================
        // ORIGINAL FILE NAME
        // =========================

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank() ||
                !originalFileName.contains(".")) {

            throw new IllegalArgumentException(
                    "Image file must have a valid extension"
            );
        }


        // =========================
        // FILE EXTENSION
        // =========================

        String extension =
                originalFileName
                        .substring(
                                originalFileName.lastIndexOf(".") + 1
                        )
                        .toLowerCase();


        // =========================
        // ALLOWED EXTENSION CHECK
        // =========================

        String expectedContentType =
                ALLOWED_IMAGE_TYPES.get(extension);

        if (expectedContentType == null) {

            throw new IllegalArgumentException(
                    "Only JPG, JPEG, PNG and WEBP images are allowed"
            );
        }


        // =========================
        // CONTENT TYPE CHECK
        // =========================

        String contentType =
                file.getContentType();

        if (contentType == null) {

            throw new IllegalArgumentException(
                    "Image content type is required"
            );
        }


        // =========================
        // EXTENSION + MIME MATCH
        // =========================

        if (!expectedContentType.equalsIgnoreCase(contentType)) {

            throw new IllegalArgumentException(
                    "Image extension and content type do not match"
            );
        }


        try {

            // =========================
            // CREATE DIRECTORY
            // =========================

            Files.createDirectories(uploadDirectory);


            // =========================
            // SERVER GENERATED FILE NAME
            // =========================

            String fileName =
                    UUID.randomUUID() + "." + extension;


            // =========================
            // SAFE TARGET PATH
            // =========================

            Path targetPath =
                    uploadDirectory
                            .resolve(fileName)
                            .normalize();


            // =========================
            // PATH SECURITY CHECK
            // =========================

            if (!targetPath.startsWith(uploadDirectory)) {

                throw new IllegalArgumentException(
                        "Invalid image file path"
                );
            }


            // =========================
            // SAVE FILE
            // =========================

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            // =========================
            // PRIMARY IMAGE
            // =========================

            if (primary) {

                List<ProductImage> existingImages =
                        imageRepository.findByProductId(productId);

                existingImages.forEach(image ->
                        image.setPrimary(false)
                );
            }


            // =========================
            // IMAGE URL
            // =========================

            String imageUrl =
                    "/uploads/products/" + fileName;


            // =========================
            // DATABASE RECORD
            // =========================

            ProductImage image =
                    ProductImage.builder()
                            .product(product)
                            .imageUrl(imageUrl)
                            .primary(primary)
                            .sortOrder(sortOrder)
                            .createdAt(LocalDateTime.now())
                            .build();


            ProductImage savedImage =
                    imageRepository.save(image);


            // =========================
            // RESPONSE
            // =========================

            return mapToDTO(savedImage);


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to upload product image",
                    e
            );
        }
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


        try {

            String imageUrl =
                    image.getImageUrl();


            // =========================
            // FILE URL CHECK
            // =========================

            if (imageUrl != null &&
                    imageUrl.startsWith(
                            "/uploads/products/"
                    )) {


                // =========================
                // EXTRACT FILE NAME
                // =========================

                String fileName =
                        imageUrl.substring(
                                "/uploads/products/".length()
                        );


                // =========================
                // SAFE FILE PATH
                // =========================

                Path filePath =
                        uploadDirectory
                                .resolve(fileName)
                                .normalize();


                // =========================
                // PATH SECURITY CHECK
                // =========================

                if (!filePath.startsWith(uploadDirectory)) {

                    throw new IllegalArgumentException(
                            "Invalid image file path"
                    );
                }


                // =========================
                // DELETE PHYSICAL FILE
                // =========================

                Files.deleteIfExists(filePath);
            }


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to delete image file",
                    e
            );
        }


        // =========================
        // DELETE DATABASE RECORD
        // =========================

        imageRepository.delete(image);
    }
}