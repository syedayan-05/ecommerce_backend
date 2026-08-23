package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.ProductCategoryDTO;
import com.ayan.ecommerce.dto.ProductRequestDTO;
import com.ayan.ecommerce.dto.ProductResponseDTO;
import com.ayan.ecommerce.entity.Category;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.CategoryRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;


    // =========================
    // ENTITY -> RESPONSE DTO
    // =========================

    private ProductResponseDTO mapToResponse(Product product) {

        ProductCategoryDTO categoryDTO = null;

        if (product.getCategory() != null) {

            categoryDTO = ProductCategoryDTO.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .build();
        }

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .createdAt(product.getCreatedAt())
                .imageUrl(product.getImageUrl())
                .category(categoryDTO)
                .build();
    }


    // =========================
    // REQUEST DTO -> ENTITY
    // =========================

    private Product mapToEntity(ProductRequestDTO dto) {

        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrl(dto.getImageUrl())
                .build();
    }


    // =========================
    // CREATE PRODUCT
    // =========================

    @Transactional
    public ProductResponseDTO saveProduct(ProductRequestDTO dto) {

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id " + dto.getCategoryId()
                        ));

        Product product = mapToEntity(dto);

        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());

        Product savedProduct = repository.save(product);

        return mapToResponse(savedProduct);
    }


    // =========================
    // GET ALL PRODUCTS
    // =========================

    public Page<ProductResponseDTO> getAllProduct(
            int page,
            int size,
            String sortBy) {

        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy)
        );

        Page<Product> products =
                repository.findAll(pageable);

        return products.map(this::mapToResponse);
    }


    // =========================
    // GET PRODUCT BY ID
    // =========================

    public ProductResponseDTO getProductById(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product Not Found with id " + id
                        ));

        return mapToResponse(product);
    }


    // =========================
    // GET PRODUCTS BY CATEGORY
    // =========================

    public List<ProductResponseDTO> getProductsByCategory(
            Long categoryId) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException(
                    "Category not found with id " + categoryId
            );
        }

        return repository.findByCategoryId(categoryId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================
    // UPDATE PRODUCT
    // =========================

    @Transactional
    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO dto) {

        Product existingProduct = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product Not Found with id " + id
                        ));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id "
                                        + dto.getCategoryId()
                        ));

        existingProduct.setName(dto.getName());
        existingProduct.setDescription(dto.getDescription());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setStock(dto.getStock());
        existingProduct.setImageUrl(dto.getImageUrl());
        existingProduct.setCategory(category);

        Product updatedProduct =
                repository.save(existingProduct);

        return mapToResponse(updatedProduct);
    }


    // =========================
    // DELETE PRODUCT
    // =========================

    @Transactional
    public void deleteProduct(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id " + id
                        ));

        repository.delete(product);
    }


    // =========================
    // SEARCH PRODUCT
    // =========================

    public List<ProductResponseDTO> searchProduct(
            String keyword) {

        return repository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}