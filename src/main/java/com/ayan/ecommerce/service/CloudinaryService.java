package com.ayan.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    //upload image
    public Map<String,Object> uploadImage(MultipartFile file){
        try{
            String publicId =
                    "sufi-leather/gallery/images/"
                        + UUID.randomUUID();

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", false
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload gallery image to Cloudinary",
                    e
            );
        }
    }

    //upload video
    public Map<String, Object> uploadVideo(
            MultipartFile file) {

        try {

            String publicId =
                    "sufi-leather/gallery/videos/"
                            + UUID.randomUUID();

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "video",
                            "overwrite", false
                    )
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to upload gallery video to Cloudinary",
                    e
            );
        }
    }

    //delete image
    public void deleteImage(
            String publicId) {

        try {

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "type", "upload",
                            "invalidate", true
                    )
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to delete gallery image from Cloudinary",
                    e
            );
        }
    }

//    delete video
    public void deleteVideo(
            String publicId) {

        try {

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "video",
                            "type", "upload",
                            "invalidate", true
                    )
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to delete gallery video from Cloudinary",
                    e
            );
        }
    }


}
