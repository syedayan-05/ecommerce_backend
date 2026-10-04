package com.ayan.ecommerce.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public class GalleryFileValidator {

    private static final long MAX_IMAGE_SIZE =
            5L * 1024 * 1024; // 5 MB

    private static final long MAX_VIDEO_SIZE =
            100L * 1024 * 1024; // 100 MB

    private static final Map<String, String> ALLOWED_TYPES =
            Map.ofEntries(

                    Map.entry(
                            "jpg",
                            "image/jpeg"
                    ),

                    Map.entry(
                            "jpeg",
                            "image/jpeg"
                    ),

                    Map.entry(
                            "png",
                            "image/png"
                    ),

                    Map.entry(
                            "webp",
                            "image/webp"
                    ),

                    Map.entry(
                            "mp4",
                            "video/mp4"
                    ),

                    Map.entry(
                            "webm",
                            "video/webm"
                    ),

                    Map.entry(
                            "mov",
                            "video/quicktime"
                    )
            );

    private final Tika tika = new Tika();

    public void validate(MultipartFile file) {

        validateFileExists(file);

        String extension =
                extractExtension(
                        file.getOriginalFilename()
                );

        String declaredContentType =
                normalizeContentType(
                        file.getContentType()
                );

        validateFileSize(
                file,
                extension
        );

        String expectedContentType =
                ALLOWED_TYPES.get(extension);

        if (expectedContentType == null) {

            throw new IllegalArgumentException(
                    "Unsupported file format"
            );
        }

        if (!expectedContentType.equals(
                declaredContentType
        )) {

            throw new IllegalArgumentException(
                    "File extension and declared content type do not match"
            );
        }

        String actualContentType =
                detectActualContentType(file);

        if (!expectedContentType.equals(
                actualContentType
        )) {

            throw new IllegalArgumentException(
                    "File content does not match the declared file type"
            );
        }
    }

    private void validateFileExists(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "File is required"
            );
        }
    }

    private void validateFileSize(
            MultipartFile file,
            String extension
    ) {

        long maxSize;

        if (isImage(extension)) {

            maxSize = MAX_IMAGE_SIZE;

        } else if (isVideo(extension)) {

            maxSize = MAX_VIDEO_SIZE;

        } else {

            throw new IllegalArgumentException(
                    "Unsupported file format"
            );
        }

        if (file.getSize() > maxSize) {

            String limit =
                    isImage(extension)
                            ? "5 MB"
                            : "100 MB";

            throw new IllegalArgumentException(
                    "File size must not exceed "
                            + limit
            );
        }
    }

    private String detectActualContentType(
            MultipartFile file
    ) {

        try {

            String detectedType =
                    tika.detect(
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
                    "Unable to validate uploaded file",
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
                    "File content type is required"
            );
        }

        return contentType
                .trim()
                .toLowerCase();
    }

    private String extractExtension(
            String filename
    ) {

        if (filename == null
                || filename.isBlank()) {

            throw new IllegalArgumentException(
                    "Filename is required"
            );
        }

        String cleanFilename =
                filename.trim();

        int lastDot =
                cleanFilename.lastIndexOf('.');

        if (lastDot <= 0
                || lastDot == cleanFilename.length() - 1) {

            throw new IllegalArgumentException(
                    "File must have a valid extension"
            );
        }

        return cleanFilename
                .substring(lastDot + 1)
                .toLowerCase();
    }

    private boolean isImage(
            String extension
    ) {

        return extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("png")
                || extension.equals("webp");
    }

    private boolean isVideo(
            String extension
    ) {

        return extension.equals("mp4")
                || extension.equals("webm")
                || extension.equals("mov");
    }
}