package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.ProductRequestDTO;
import com.ayan.ecommerce.dto.ProductResponseDTO;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")

public class ProductController {
    @Autowired
    private ProductService service;

    @PostMapping
    public ProductResponseDTO createProduct(
            @Valid  @RequestBody ProductRequestDTO dto){
        return service.saveProduct(dto);
    }

    @GetMapping("/all")
    public List<Product> getAllProduct(){
        return service.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getAllProduct(@PathVariable Long id){
        return service.getProductById(id);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id,
                                 @RequestBody Product product){
        return service.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id){
        service.deleteProduct(id);
        return "Product deleted successfully";
    }

    @GetMapping
    public Page<ProductResponseDTO> getAllProduct(
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "5")
            int size,
            @RequestParam(defaultValue = "id")
            String sortBy){

        return service.getAllProduct(page, size, sortBy);
    }

    @GetMapping("/search")
    public List<ProductResponseDTO> searchProduct(@RequestParam String keyword){
        return service.searchProduct(keyword);
    }
}
