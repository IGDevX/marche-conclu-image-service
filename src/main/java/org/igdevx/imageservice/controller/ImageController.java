package org.igdevx.imageservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.imageservice.dto.ErrorResponse;
import org.igdevx.imageservice.dto.ImageResponse;
import org.igdevx.imageservice.dto.UploadResponse;
import org.igdevx.imageservice.model.Image;
import org.igdevx.imageservice.service.ImageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Management", description = "API for image management (upload, download, delete)")
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/upload/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a profile picture",
            description = "Upload a user profile image to MinIO and save metadata"
    )
    @ApiResponse(responseCode = "200", description = "Image uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UploadResponse> uploadProfileImage(
            @Parameter(description = "Image file to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "User ID") @RequestParam("userId") String userId
    ) {
        try {
            UploadResponse response = imageService.uploadImage(
                    file,
                    Image.EntityType.USER_PROFILE,
                    userId,
                    null
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Profile image validation failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Failed to upload profile image for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/upload/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a user banner",
            description = "Upload a user banner image to MinIO and save metadata"
    )
    @ApiResponse(responseCode = "200", description = "Banner uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UploadResponse> uploadBannerImage(
            @Parameter(description = "Image file to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "User ID") @RequestParam("userId") String userId
    ) {
        try {
            UploadResponse response = imageService.uploadImage(
                    file,
                    Image.EntityType.USER_BANNER,
                    userId,
                    null
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Banner image validation failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Failed to upload banner image for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/upload/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a product image",
            description = "Upload a product image to MinIO and save metadata"
    )
    @ApiResponse(responseCode = "200", description = "Product image uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UploadResponse> uploadProductImage(
            @Parameter(description = "Image file to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Product ID") @RequestParam("productId") String productId,
            @Parameter(description = "User ID (product owner)") @RequestParam("userId") String userId
    ) {
        try {
            UploadResponse response = imageService.uploadImage(
                    file,
                    Image.EntityType.PRODUCT,
                    userId,
                    productId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Product image validation failed for product {}: {}", productId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Failed to upload product image for product {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get image metadata",
            description = "Returns image information by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Image metadata")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<ImageResponse> getImage(
            @Parameter(description = "Image ID") @PathVariable UUID id
    ) {
        try {
            ImageResponse response = imageService.getImageById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to retrieve image metadata: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @Operation(
            summary = "List all images",
            description = "Returns all non-deleted images"
    )
    @ApiResponse(responseCode = "200", description = "List of images")
    public ResponseEntity<List<ImageResponse>> getAllImages() {
        List<ImageResponse> images = imageService.getAllImages();
        return ResponseEntity.ok(images);
    }

    @GetMapping("/user/{userId}/profile")
    @Operation(
            summary = "Get user profile image",
            description = "Returns the profile image of a user"
    )
    @ApiResponse(responseCode = "200", description = "User profile image")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<ImageResponse> getUserProfileImage(
            @Parameter(description = "User ID") @PathVariable String userId
    ) {
        ImageResponse image = imageService.getImageByUserAndType(userId, Image.EntityType.USER_PROFILE);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    @GetMapping("/user/{userId}/banner")
    @Operation(
            summary = "Get user banner image",
            description = "Returns the banner image of a user"
    )
    @ApiResponse(responseCode = "200", description = "User banner image")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<ImageResponse> getUserBannerImage(
            @Parameter(description = "User ID") @PathVariable String userId
    ) {
        ImageResponse image = imageService.getImageByUserAndType(userId, Image.EntityType.USER_BANNER);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    @GetMapping("/product/{productId}")
    @Operation(
            summary = "Get product image",
            description = "Returns the image of a product"
    )
    @ApiResponse(responseCode = "200", description = "Product image")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<ImageResponse> getProductImage(
            @Parameter(description = "Product ID") @PathVariable String productId
    ) {
        ImageResponse image = imageService.getImageByProductId(productId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get all images of a user",
            description = "Returns all images (profile, banner, products) belonging to a user"
    )
    @ApiResponse(responseCode = "200", description = "List of user images")
    public ResponseEntity<List<ImageResponse>> getImagesByUser(
            @Parameter(description = "User ID") @PathVariable String userId
    ) {
        List<ImageResponse> images = imageService.getImagesByUser(userId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}/download")
    @Operation(
            summary = "Download an image",
            description = "Download image file from MinIO"
    )
    @ApiResponse(responseCode = "200", description = "Image file", content = @Content(mediaType = "image/*"))
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<InputStreamResource> downloadImage(
            @Parameter(description = "Image ID") @PathVariable UUID id
    ) {
        try {
            ImageResponse imageInfo = imageService.getImageById(id);
            InputStream inputStream = imageService.downloadImage(id);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(imageInfo.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + imageInfo.getFileName() + "\"")
                    .body(new InputStreamResource(inputStream));

        } catch (RuntimeException e) {
            log.error("Image download failed, not found in database: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Image download failed, MinIO error for image: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an image",
            description = "Delete an image from MinIO and mark as deleted in database"
    )
    @ApiResponse(responseCode = "204", description = "Image deleted successfully")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Image ID") @PathVariable UUID id
    ) {
        try {
            imageService.deleteImage(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Cannot delete image, not found in database: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Image deletion failed, MinIO error for image: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/product/{productId}")
    @Operation(
            summary = "Delete product image",
            description = "Delete the image associated with a product"
    )
    @ApiResponse(responseCode = "204", description = "Product image deleted successfully")
    @ApiResponse(responseCode = "404", description = "Product image not found")
    public ResponseEntity<Void> deleteProductImage(
            @Parameter(description = "Product ID") @PathVariable String productId
    ) {
        try {
            imageService.deleteImageByProductId(productId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.error("Failed to delete product image: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/user/{userId}")
    @Operation(
            summary = "Delete all images of a user",
            description = "Delete all images (profile, banner, products) belonging to a user from MinIO and database"
    )
    @ApiResponse(responseCode = "204", description = "User images deleted successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<Void> deleteAllUserImages(
            @Parameter(description = "User ID") @PathVariable String userId
    ) {
        try {
            imageService.deleteAllUserImages(userId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.error("Failed to delete all images for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Check if the service is running properly"
    )
    @ApiResponse(responseCode = "200", description = "Service operational")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Image Service is running");
    }
}

