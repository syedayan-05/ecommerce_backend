package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.ProductFilterDTO;
import com.ayan.ecommerce.dto.ProductRequestDTO;
import com.ayan.ecommerce.dto.ProductResponseDTO;
import com.ayan.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    // =========================
    // ADMIN APIs
    // =========================

    @PostMapping
    public ProductResponseDTO createProduct(
            @Valid @RequestBody ProductRequestDTO dto) {

        return service.saveProduct(dto);
    }

    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto) {

        return service.updateProduct(id, dto);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(
            @PathVariable Long id) {

        service.deleteProduct(id);

        return "Product deleted successfully";
    }


    // =========================
    // PUBLIC APIs
    // =========================

    @GetMapping
    public Page<ProductResponseDTO> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        return service.getAllProduct(page, size, sortBy);
    }

    @GetMapping("/filter")
    public Page<ProductResponseDTO> filterProducts(

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) Long categoryId,

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(required = false) Integer minStock,

            @RequestParam(required = false) Integer maxStock,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "5") int size,

            @RequestParam(defaultValue = "id") String sortBy) {

        ProductFilterDTO filter = ProductFilterDTO.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minStock(minStock)
                .maxStock(maxStock)
                .build();

        return service.filterProducts(
                filter,
                page,
                size,
                sortBy
        );
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(
            @PathVariable Long id) {

        return service.getProductById(id);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponseDTO> getProductsByCategory(
            @PathVariable Long categoryId) {

        return service.getProductsByCategory(categoryId);
    }

    @GetMapping("/search")
    public List<ProductResponseDTO> searchProduct(
            @RequestParam String keyword) {

        return service.searchProduct(keyword);
    }
}