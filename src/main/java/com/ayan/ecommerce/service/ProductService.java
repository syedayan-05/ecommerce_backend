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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;

    private ProductResponseDTO mapToResponse(Product product) {

        ProductCategoryDTO categoryDTO = null;

        if(product.getCategory() != null){

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
                .category(categoryDTO)
                .build();
    }

    private Product mapToEntity(ProductRequestDTO dto){
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .build();

    }

    public ProductResponseDTO saveProduct(ProductRequestDTO dto){
        Category category =
                categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(()->
                                new RuntimeException("category not found"));
        Product product = mapToEntity(dto);
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());
        Product savedProduct = repository.save(product);
        return mapToResponse(savedProduct);
    }

    public List<ProductResponseDTO> getAllProducts(){
        return repository.findAll().
                stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Product getProductById(Long id){
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product Not Found with id " + id));
    }

    public Product updateProduct(Long id,Product updateProduct){
        Product existingProduct =  repository.findById(id)
                .orElseThrow(()->
                        new ProductNotFoundException(
                                "Product Not Found with id " + id
                        )
                );
        existingProduct.setName(updateProduct.getName());
        existingProduct.setDescription(updateProduct.getDescription());
        existingProduct.setPrice(updateProduct.getPrice());
        existingProduct.setStock(updateProduct.getStock());

        return repository.save(existingProduct);
    }

    public void deleteProduct(Long id){
        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                           "Product not found with id " + id
                        ));

        repository.delete(product);
    }

    public Page<ProductResponseDTO> getAllProduct(
            int page,
            int size,
            String sortBy){
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy)
        );

        Page<Product> products =
                repository.findAll(pageable);

        return products.map(this::mapToResponse);

    }

    public List<ProductResponseDTO> searchProduct(String keyword){
            return repository.findByNameContainingIgnoreCase(keyword)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
    }

}
