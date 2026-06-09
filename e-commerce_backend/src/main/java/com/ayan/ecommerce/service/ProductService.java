package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.ProductRequestDTO;
import com.ayan.ecommerce.dto.ProductResponseDTO;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    private ProductResponseDTO mapToResponse(Product product) {

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .createdAt(product.getCreatedAt())
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
        Product product = mapToEntity(dto);
        product.setCreatedAt(LocalDateTime.now());
        Product savedProduct = repository.save(product);
        return mapToResponse(savedProduct);
    }

    public List<Product> getAllProducts(){
        return repository.findAll();
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
}
