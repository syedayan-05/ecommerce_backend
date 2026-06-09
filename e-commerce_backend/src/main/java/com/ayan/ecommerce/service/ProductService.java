package com.ayan.ecommerce.service;

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

    public Product saveProduct(Product product){
        product.setCreatedAt(LocalDateTime.now());
        return repository.save(product);
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
