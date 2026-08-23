package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.CategoryRequest;
import com.ayan.ecommerce.dto.CategoryResponse;
import com.ayan.ecommerce.entity.Category;
import com.ayan.ecommerce.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;


    private CategoryResponse mapToResponse(
            Category category
    ) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }


    private Category mapToEntity(
            CategoryRequest request
    ) {

        return Category.builder()
                .name(request.getName().trim())
                .description(
                        request.getDescription() == null
                                ? null
                                : request.getDescription().trim()
                )
                .build();
    }


    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse createCategory(
            CategoryRequest request
    ) {

        String categoryName =
                request.getName().trim();


        if (repository.existsByNameIgnoreCase(
                categoryName
        )) {

            throw new IllegalArgumentException(
                    "Category already exists"
            );
        }


        Category category =
                mapToEntity(request);


        Category savedCategory =
                repository.save(category);


        return mapToResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(){
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id){
        Category category = repository.findById(id)
                .orElseThrow(()->
                        new EntityNotFoundException("Category not found with id " + id
                        ));

        return mapToResponse(category);
    }

    public CategoryResponse updateCategory(Long id ,
                                           CategoryRequest request){
        Category category = repository.findById(id)
                .orElseThrow(()->
                        new EntityNotFoundException("Category not found with id " + id
                        ));

        category.setName(request.getName().trim());
        category.setDescription(
                request.getDescription() != null
                        ? request.getDescription().trim() : null
        );

        Category updateCategory = repository.save(category);

        return mapToResponse(updateCategory);
    }

    public void deleteCategory(Long id){
        Category category = repository.findById(id)
                .orElseThrow(()->
                        new EntityNotFoundException("Category not found with id " + id
                        ));

        repository.delete(category);
    }
}

