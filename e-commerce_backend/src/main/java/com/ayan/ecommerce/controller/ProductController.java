package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.service.ProductService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")

public class ProductController {
    @Autowired
    private ProductService service;

    @PostMapping
    public Product createProduct(@RequestBody Product product){
        return service.saveProduct(product);
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
}
