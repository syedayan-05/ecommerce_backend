package com.ayan.ecommerce.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Component
public class GalleryFileValidator {

    // =========================================================
    // FILE SIZE LIMITS
    // =========================================================

    private static final long MAX_IMAGE_SIZE =
            5L * 1024 * 1024; // 5 MB

    private static final long MAX_VIDEO_SIZE =
            100L * 1024 * 1024; // 100 MB


    // =========================================================
    // ALLOWED MIME TYPES + EXTENSIONS
    // =========================================================

    private static final Map<String, Set<String>> ALLOWED_IMAGE_TYPES =
            Map.of(
                    "image/jpeg", Set.of("jpg", "jpeg"),
                    "image/png", Set.of("png"),
                    "image/webp", Set.of("webp")
            );

    private static final Map<String, Set<String>> ALLOWED_VIDEO_TYPES =
            Map.of(
                    "video/mp4", Set.of("mp4"),
                    "video/webm", Set.of("webm"),
                    "video/quicktime", Set.of("mov")
            );


    // =========================================================
    // IMAGE VALIDATION
    // =========================================================

    public void validateImage(MultipartFile file) {

        validateFileExists(file);

        validateFileSize(
                file,
                MAX_IMAGE_SIZE,
                "Image"
        );

        String contentType =
                normalizeContentType(file.getContentType());

        String extension =
                extractExtension(file.getOriginalFilename());

        Set<String> allowedExtensions =
                ALLOWED_IMAGE_TYPES.get(contentType);

        if (allowedExtensions == null) {

            throw new IllegalArgumentException(
                    "Unsupported image type. Allowed types: JPG, JPEG, PNG, WEBP"
            );
        }

        if (!allowedExtensions.contains(extension)) {

            throw new IllegalArgumentException(
                    "File extension does not match the image content type"
            );
        }
    }


    // =========================================================
    // VIDEO VALIDATION
    // =========================================================

    public void validateVideo(MultipartFile file) {

        validateFileExists(file);

        validateFileSize(
                file,
                MAX_VIDEO_SIZE,
                "Video"
        );

        String contentType =
                normalizeContentType(file.getContentType());

        String extension =
                extractExtension(file.getOriginalFilename());

        Set<String> allowedExtensions =
                ALLOWED_VIDEO_TYPES.get(contentType);

        if (allowedExtensions == null) {

            throw new IllegalArgumentException(
                    "Unsupported video type. Allowed types: MP4, WEBM, MOV"
            );
        }

        if (!allowedExtensions.contains(extension)) {

            throw new IllegalArgumentException(
                    "File extension does not match the video content type"
            );
        }
    }


    // =========================================================
    // FILE EXISTS CHECK
    // =========================================================

    private void validateFileExists(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Gallery file is required"
            );
        }
    }


    // =========================================================
    // FILE SIZE CHECK
    // =========================================================

    private void validateFileSize(
            MultipartFile file,
            long maxSize,
            String fileType) {

        if (file.getSize() > maxSize) {

            long maxSizeInMb =
                    maxSize / (1024 * 1024);

            throw new IllegalArgumentException(
                    fileType
                            + " size cannot exceed "
                            + maxSizeInMb
                            + " MB"
            );
        }
    }


    // =========================================================
    // MIME TYPE NORMALIZATION
    // =========================================================

    private String normalizeContentType(
            String contentType) {

        if (contentType == null) {

            throw new IllegalArgumentException(
                    "Unable to determine file content type"
            );
        }

        return contentType
                .trim()
                .toLowerCase();
    }


    // =========================================================
    // FILE EXTENSION EXTRACTION
    // =========================================================

    private String extractExtension(
            String originalFilename) {

        if (originalFilename == null
                || originalFilename.isBlank()) {

            throw new IllegalArgumentException(
                    "Original file name is required"
            );
        }

        String fileName =
                originalFilename.trim();

        int lastDot =
                fileName.lastIndexOf('.');

        if (lastDot <= 0
                || lastDot == fileName.length() - 1) {

            throw new IllegalArgumentException(
                    "File must have a valid extension"
            );
        }

        return fileName
                .substring(lastDot + 1)
                .toLowerCase();
    }
}