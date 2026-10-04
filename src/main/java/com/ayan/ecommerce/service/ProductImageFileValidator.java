package com.ayan.ecommerce.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public class ProductImageFileValidator {

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024; // 5 MB

    private static final Map<String, String> ALLOWED_IMAGE_TYPES =
            Map.of(
                    "jpg", "image/jpeg",
                    "jpeg", "image/jpeg",
                    "png", "image/png",
                    "webp", "image/webp"
            );

    private final Tika tika = new Tika();

    public void validate(MultipartFile file) {

        validateFileExists(file);

        validateFileSize(file);

        String extension =
                extractExtension(file.getOriginalFilename());

        String declaredContentType =
                normalizeContentType(file.getContentType());

        String actualContentType =
                detectActualContentType(file);

        validateAllowedType(
                extension,
                declaredContentType,
                actualContentType
        );
    }

    private void validateFileExists(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }
    }

    private void validateFileSize(MultipartFile file) {

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB"
            );
        }
    }

    private void validateAllowedType(
            String extension,
            String declaredContentType,
            String actualContentType
    ) {

        String expectedContentType =
                ALLOWED_IMAGE_TYPES.get(extension);

        if (expectedContentType == null) {
            throw new IllegalArgumentException(
                    "Unsupported image format. Allowed formats: JPG, JPEG, PNG, WEBP"
            );
        }

        if (!expectedContentType.equals(declaredContentType)) {
            throw new IllegalArgumentException(
                    "File extension and declared content type do not match"
            );
        }

        if (!expectedContentType.equals(actualContentType)) {
            throw new IllegalArgumentException(
                    "File content does not match the declared image type"
            );
        }
    }

    private String detectActualContentType(
            MultipartFile file
    ) {

        try {

            String detectedType = tika.detect(
                    file.getInputStream(),
                    file.getOriginalFilename()
            );

            if (detectedType == null
                    || detectedType.isBlank()
                    || detectedType.equalsIgnoreCase(
                    "application/octet-stream"
            )) {

                throw new IllegalArgumentException(
                        "Unable to determine actual file type"
                );
            }

            return detectedType
                    .trim()
                    .toLowerCase();

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "Unable to validate image file",
                    e
            );
        }
    }

    private String normalizeContentType(
            String contentType
    ) {

        if (contentType == null
                || contentType.isBlank()) {

            throw new IllegalArgumentException(
                    "Image content type is required"
            );
        }

        return contentType
                .trim()
                .toLowerCase();
    }

    private String extractExtension(
            String originalFilename
    ) {

        if (originalFilename == null
                || originalFilename.isBlank()) {

            throw new IllegalArgumentException(
                    "Image filename is required"
            );
        }

        String filename =
                originalFilename.trim();

        int lastDot =
                filename.lastIndexOf('.');

        if (lastDot <= 0
                || lastDot == filename.length() - 1) {

            throw new IllegalArgumentException(
                    "Image file must have a valid extension"
            );
        }

        return filename
                .substring(lastDot + 1)
                .toLowerCase();
    }
}