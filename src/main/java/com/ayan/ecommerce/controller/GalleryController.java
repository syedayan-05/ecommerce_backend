package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.GalleryRequestDTO;
import com.ayan.ecommerce.dto.GalleryResponseDTO;
import com.ayan.ecommerce.entity.GalleryCategory;
import com.ayan.ecommerce.service.GalleryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;


    // =========================================================
    // PUBLIC GET APIs
    // =========================================================

    @GetMapping
    public List<GalleryResponseDTO> getAllGallery() {

        return galleryService.getAllGallery();
    }


    @GetMapping("/{id}")
    public GalleryResponseDTO getGalleryById(
            @PathVariable Long id) {

        return galleryService.getGalleryById(id);
    }


    @GetMapping("/category/{category}")
    public List<GalleryResponseDTO> getGalleryByCategory(
            @PathVariable GalleryCategory category) {

        return galleryService.getGalleryByCategory(category);
    }


    // =========================================================
    // ADMIN CREATE
    // =========================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = {
                            @io.swagger.v3.oas.annotations.media.Encoding(
                                    name = "data",
                                    contentType = MediaType.APPLICATION_JSON_VALUE
                            )
                    }
            )
    )
    public GalleryResponseDTO createGallery(

            @Valid
            @RequestPart("data")
            GalleryRequestDTO dto,

            @RequestPart("file")
            MultipartFile file) {

        return galleryService.createGallery(dto, file);
    }


    // =========================================================
    // ADMIN UPDATE
    // =========================================================

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = {
                            @io.swagger.v3.oas.annotations.media.Encoding(
                                    name = "data",
                                    contentType = MediaType.APPLICATION_JSON_VALUE
                            )
                    }
            )
    )
    public GalleryResponseDTO updateGallery(

            @PathVariable Long id,

            @Valid
            @RequestPart("data")
            GalleryRequestDTO dto,

            @RequestPart(value = "file", required = false)
            MultipartFile file) {

        return galleryService.updateGallery(
                id,
                dto,
                file
        );
    }


    // =========================================================
    // ADMIN DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public String deleteGallery(
            @PathVariable Long id) {

        galleryService.deleteGallery(id);

        return "Gallery item deleted successfully";
    }
}