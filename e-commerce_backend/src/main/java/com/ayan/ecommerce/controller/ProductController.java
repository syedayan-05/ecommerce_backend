package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")

public class ProductController {
    @Autowired
    private ProductService service;

    @PostMapping
    public Product createProduct(@RequestBody Product product){
        return service.saveProduct(product);
    }
}
