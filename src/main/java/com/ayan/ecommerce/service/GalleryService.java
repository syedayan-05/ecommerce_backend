package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.GalleryRequestDTO;
import com.ayan.ecommerce.dto.GalleryResponseDTO;
import com.ayan.ecommerce.entity.Gallery;
import com.ayan.ecommerce.entity.GalleryCategory;
import com.ayan.ecommerce.entity.GalleryMediaType;
import com.ayan.ecommerce.repository.GalleryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final CloudinaryService cloudinaryService;
    private final GalleryFileValidator galleryFileValidator;


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    private GalleryResponseDTO mapToDTO(Gallery gallery) {

        return GalleryResponseDTO.builder()
                .id(gallery.getId())
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .mediaUrl(gallery.getMediaUrl())
                .mediaType(gallery.getMediaType())
                .category(gallery.getCategory())
                .sortOrder(gallery.getSortOrder())
                .build();
    }


    // =========================================================
    // CREATE GALLERY
    // =========================================================

    @Transactional
    public GalleryResponseDTO createGallery(
            GalleryRequestDTO dto,
            MultipartFile file) {

        validateFile(file);

        GalleryMediaType mediaType = determineMediaType(file);

        Map<String, Object> uploadResult =
                uploadToCloudinary(file, mediaType);

        String mediaUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        if (mediaUrl == null || publicId == null) {
            throw new RuntimeException(
                    "Cloudinary upload failed: missing media information"
            );
        }

        try {

            Gallery gallery = Gallery.builder()
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .mediaUrl(mediaUrl)
                    .publicId(publicId)
                    .mediaType(mediaType)
                    .category(dto.getCategory())
                    .sortOrder(dto.getSortOrder())
                    .active(dto.isActive())
                    .createdAt(LocalDateTime.now())
                    .build();

            Gallery savedGallery =
                    galleryRepository.save(gallery);

            return mapToDTO(savedGallery);

        } catch (Exception e) {

            // DB save failed after Cloudinary upload.
            // Remove the newly uploaded Cloudinary asset
            // to avoid an orphaned file.

            deleteFromCloudinary(publicId, mediaType);

            throw new RuntimeException(
                    "Failed to save gallery item",
                    e
            );
        }
    }


    // =========================================================
    // GET ALL ACTIVE GALLERY
    // =========================================================

    @Transactional(readOnly = true)
    public List<GalleryResponseDTO> getAllGallery() {

        return galleryRepository
                .findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }


    // =========================================================
    // GET GALLERY BY CATEGORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<GalleryResponseDTO> getGalleryByCategory(
            GalleryCategory category) {

        return galleryRepository
                .findByCategoryAndActiveTrueOrderBySortOrderAsc(category)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }


    // =========================================================
    // GET GALLERY BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public GalleryResponseDTO getGalleryById(Long id) {

        Gallery gallery = galleryRepository
                .findById(id)
                .filter(Gallery::isActive)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Gallery item not found with id " + id
                        )
                );

        return mapToDTO(gallery);
    }


    // =========================================================
    // UPDATE GALLERY
    // =========================================================

    @Transactional
    public GalleryResponseDTO updateGallery(
            Long id,
            GalleryRequestDTO dto,
            MultipartFile file) {

        Gallery gallery = galleryRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Gallery item not found with id " + id
                        )
                );

        // ---------------------------------------------
        // Update basic metadata
        // ---------------------------------------------

        gallery.setTitle(dto.getTitle());
        gallery.setDescription(dto.getDescription());
        gallery.setCategory(dto.getCategory());
        gallery.setSortOrder(dto.getSortOrder());
        gallery.setActive(dto.isActive());


        // ---------------------------------------------
        // Replace media only if a new file was provided
        // ---------------------------------------------

        if (file != null && !file.isEmpty()) {

            validateFile(file);

            GalleryMediaType newMediaType =
                    determineMediaType(file);

            Map<String, Object> uploadResult =
                    uploadToCloudinary(file, newMediaType);

            String newMediaUrl =
                    (String) uploadResult.get("secure_url");

            String newPublicId =
                    (String) uploadResult.get("public_id");

            if (newMediaUrl == null || newPublicId == null) {

                throw new RuntimeException(
                        "Cloudinary upload failed: missing media information"
                );
            }

            // Keep old media information until DB update succeeds
            String oldPublicId = gallery.getPublicId();
            GalleryMediaType oldMediaType = gallery.getMediaType();

            try {

                gallery.setMediaUrl(newMediaUrl);
                gallery.setPublicId(newPublicId);
                gallery.setMediaType(newMediaType);

                Gallery updatedGallery =
                        galleryRepository.save(gallery);

                // -----------------------------------------
                // DB successfully updated.
                // Now remove old Cloudinary asset.
                // -----------------------------------------

                if (oldPublicId != null && !oldPublicId.isBlank()) {

                    try {

                        deleteFromCloudinary(
                                oldPublicId,
                                oldMediaType
                        );

                    } catch (Exception cleanupException) {

                        // Do not invalidate the successful DB update
                        // because the old Cloudinary asset could not
                        // be removed.

                        System.err.println(
                                "Warning: Old Cloudinary asset could not be deleted. "
                                        + "publicId=" + oldPublicId
                        );
                    }
                }

                return mapToDTO(updatedGallery);

            } catch (Exception e) {

                // DB update failed.
                // Delete newly uploaded asset because DB still
                // points to the old asset.

                deleteFromCloudinary(
                        newPublicId,
                        newMediaType
                );

                throw new RuntimeException(
                        "Failed to update gallery item",
                        e
                );
            }
        }


        // ---------------------------------------------
        // Metadata-only update
        // ---------------------------------------------

        Gallery updatedGallery =
                galleryRepository.save(gallery);

        return mapToDTO(updatedGallery);
    }


    // =========================================================
    // DELETE GALLERY
    // =========================================================

    @Transactional
    public void deleteGallery(Long id) {

        Gallery gallery = galleryRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Gallery item not found with id " + id
                        )
                );

        String publicId = gallery.getPublicId();
        GalleryMediaType mediaType = gallery.getMediaType();

        // ---------------------------------------------
        // Delete DB record first
        // ---------------------------------------------

        galleryRepository.delete(gallery);

        // ---------------------------------------------
        // Then delete Cloudinary asset
        // ---------------------------------------------

        if (publicId != null && !publicId.isBlank()) {

            try {

                deleteFromCloudinary(
                        publicId,
                        mediaType
                );

            } catch (Exception e) {

                // DB record is already removed.
                // Log the cleanup problem for later handling.
                System.err.println(
                        "Warning: Gallery deleted from database, "
                                + "but Cloudinary asset could not be deleted. "
                                + "publicId=" + publicId
                );
            }
        }
    }


    // =========================================================
    // FILE VALIDATION
    // =========================================================

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Gallery file is required"
            );
        }
    }


    // =========================================================
    // DETERMINE MEDIA TYPE
    // =========================================================

    private GalleryMediaType determineMediaType(
            MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType == null) {

            throw new IllegalArgumentException(
                    "Unable to determine file type"
            );
        }

        if (contentType.startsWith("image/")) {

            galleryFileValidator.validateImage(file);

            return GalleryMediaType.IMAGE;
        }

        if (contentType.startsWith("video/")) {

            galleryFileValidator.validateVideo(file);

            return GalleryMediaType.VIDEO;
        }

        throw new IllegalArgumentException(
                "Unsupported gallery media type: " + contentType
        );
    }


    // =========================================================
    // CLOUDINARY UPLOAD
    // =========================================================

    private Map<String, Object> uploadToCloudinary(
            MultipartFile file,
            GalleryMediaType mediaType) {

        if (mediaType == GalleryMediaType.IMAGE) {

            return cloudinaryService.uploadImage(file);
        }

        return cloudinaryService.uploadVideo(file);
    }


    // =========================================================
    // CLOUDINARY DELETE
    // =========================================================

    private void deleteFromCloudinary(
            String publicId,
            GalleryMediaType mediaType) {

        if (mediaType == GalleryMediaType.IMAGE) {

            cloudinaryService.deleteImage(publicId);

        } else if (mediaType == GalleryMediaType.VIDEO) {

            cloudinaryService.deleteVideo(publicId);
        }
    }
}