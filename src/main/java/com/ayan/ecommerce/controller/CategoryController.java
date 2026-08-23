package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.CategoryRequest;
import com.ayan.ecommerce.dto.CategoryResponse;
import com.ayan.ecommerce.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public List<CategoryResponse> getAllCategories()
    {
        return service.getAllCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById( @PathVariable Long id )
    {
        return service.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @Valid @RequestBody CategoryRequest request
    )
    {
        return service.createCategory(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ){
        return service.updateCategory(id,request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
        @PathVariable Long id
    ){
        service.deleteCategory(id);
    }




}

