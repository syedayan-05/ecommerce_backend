package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.CategoryRequest;
import com.ayan.ecommerce.dto.CategoryResponse;
import com.ayan.ecommerce.service.CategoryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @PostMapping
    public CategoryResponse categoryCreate(@RequestBody CategoryRequest request){
        return service.createCategory(request);
    }
}
