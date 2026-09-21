package com.ayan.ecommerce.dto;

import com.ayan.ecommerce.entity.GalleryCategory;
import com.ayan.ecommerce.entity.GalleryMediaType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String mediaUrl;
    private GalleryMediaType mediaType;
    private GalleryCategory category;
    private Integer sortOrder;
}