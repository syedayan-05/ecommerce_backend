package com.ayan.ecommerce.dto;

import com.ayan.ecommerce.entity.GalleryCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Gallery category is required")
    private GalleryCategory category;

    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be zero or greater")
    private Integer sortOrder;

    private boolean active;
}