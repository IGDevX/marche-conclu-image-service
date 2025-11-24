package org.igdevx.imageservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.imageservice.dto.ImageResponse;
import org.igdevx.imageservice.dto.UploadResponse;
import org.igdevx.imageservice.model.Image;
import org.igdevx.imageservice.repository.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final MinioService minioService;

    @Transactional
    public UploadResponse uploadImage(
            MultipartFile file,
            Image.EntityType entityType,
            String userId,
            String productId
    ) throws IOException {

        validateImageFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String cloudPath = buildCloudPath(userId, entityType, fileName);

        minioService.uploadFile(file, cloudPath);

        Image image = Image.builder()
                .entityType(entityType)
                .userId(userId)
                .productId(productId)
                .cloudPath(cloudPath)
                .fileName(fileName)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();

        image = imageRepository.save(image);

        log.info("Image uploaded successfully: {} for user {}", fileName, userId);

        return UploadResponse.builder()
                .imageId(image.getId().toString())
                .fileName(fileName)
                .cloudPath(cloudPath)
                .url(minioService.getPresignedUrl(cloudPath))
                .sizeBytes(file.getSize())
                .message("Image uploaded successfully")
                .build();
    }

    public ImageResponse getImageById(UUID id) {
        Image image = imageRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        return toImageResponse(image);
    }

    public ImageResponse getImageByUserAndType(String userId, Image.EntityType entityType) {
        List<Image> images = imageRepository.findByUserIdAndEntityTypeAndDeletedAtIsNull(userId, entityType);
        if (images.isEmpty()) {
            return null;
        }
        return toImageResponse(images.get(0));
    }

    public ImageResponse getImageByProductId(String productId) {
        return imageRepository.findByProductIdAndDeletedAtIsNull(productId)
                .map(this::toImageResponse)
                .orElse(null);
    }

    public List<ImageResponse> getAllImages() {
        return imageRepository.findByDeletedAtIsNull()
                .stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    public List<ImageResponse> getImagesByUser(String userId) {
        return imageRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    public InputStream downloadImage(UUID id) throws IOException {
        Image image = imageRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        return minioService.downloadFile(image.getCloudPath());
    }

    @Transactional
    public void deleteImage(UUID id) throws IOException {
        Image image = imageRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        image.setDeletedAt(LocalDateTime.now());
        imageRepository.save(image);

        try {
            minioService.deleteFile(image.getCloudPath());
            log.info("Image deleted: {}", id);
        } catch (IOException e) {
            log.error("Error deleting file from MinIO, but marked as deleted in DB", e);
        }
    }

    @Transactional
    public void deleteImageByProductId(String productId) throws IOException {
        imageRepository.findByProductIdAndDeletedAtIsNull(productId).ifPresent(image -> {
            image.setDeletedAt(LocalDateTime.now());
            imageRepository.save(image);

            try {
                minioService.deleteFile(image.getCloudPath());
                log.info("Product image deleted: {}", productId);
            } catch (IOException e) {
                log.error("Error deleting product image {} from MinIO", image.getCloudPath(), e);
            }
        });
    }

    @Transactional
    public void deleteAllUserImages(String userId) throws IOException {
        List<Image> images = imageRepository.findByUserIdAndDeletedAtIsNull(userId);

        for (Image image : images) {
            image.setDeletedAt(LocalDateTime.now());
        }
        imageRepository.saveAll(images);

        try {
            String userProfilePath = String.format("users/%s/", userId);
            String userProductPath = String.format("products/%s/", userId);

            minioService.deleteFolder(userProfilePath);
            minioService.deleteFolder(userProductPath);

            log.info("All images deleted for user: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting user images from MinIO: {}", userId, e);
            throw new IOException("User images deletion failed", e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 10 MB");
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String buildCloudPath(String userId, Image.EntityType entityType, String fileName) {
        return switch (entityType) {
            case USER_PROFILE -> String.format("users/%s/profile%s", userId, getExtension(fileName));
            case USER_BANNER -> String.format("users/%s/banner%s", userId, getExtension(fileName));
            case PRODUCT -> String.format("products/%s/%s", userId, fileName);
        };
    }

    private String getExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return ".jpg";
    }

    private ImageResponse toImageResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getId())
                .entityType(image.getEntityType().name())
                .userId(image.getUserId())
                .productId(image.getProductId())
                .cloudPath(image.getCloudPath())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .sizeBytes(image.getSizeBytes())
                .uploadedAt(image.getUploadedAt())
                .url(minioService.getPresignedUrl(image.getCloudPath()))
                .build();
    }
}

