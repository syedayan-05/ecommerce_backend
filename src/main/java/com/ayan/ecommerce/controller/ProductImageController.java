package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.ProductImageDTO;
import com.ayan.ecommerce.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService service;

    // =========================
    // UPLOAD PRODUCT IMAGE
    // =========================

    @PostMapping(
            value = "/{productId}/images/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ProductImageDTO uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primary,
            @RequestParam(defaultValue = "0") Integer sortOrder) {

        return service.uploadImage(
                productId,
                file,
                primary,
                sortOrder
        );
    }


    // =========================
    // GET PRODUCT IMAGES
    // =========================

    @GetMapping("/{productId}/images")
    public List<ProductImageDTO> getProductImages(
            @PathVariable Long productId) {

        return service.getProductImages(productId);
    }


    // =========================
    // DELETE PRODUCT IMAGE
    // =========================

    @DeleteMapping("/images/{imageId}")
    public String deleteImage(
            @PathVariable Long imageId) {

        service.deleteImage(imageId);

        return "Product image deleted successfully";
    }
}