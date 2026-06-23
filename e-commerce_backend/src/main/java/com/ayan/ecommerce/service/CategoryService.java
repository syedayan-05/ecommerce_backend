package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.CategoryRequest;
import com.ayan.ecommerce.dto.CategoryResponse;
import com.ayan.ecommerce.entity.Category;
import com.ayan.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    private CategoryResponse mapToResponse(Category category){
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private Category mapToEntity(CategoryRequest request){
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public CategoryResponse createCategory(CategoryRequest request){
        Category category =  mapToEntity(request);
        Category saveToCategory = repository.save(category);
        return  mapToResponse(saveToCategory);
    }
}
