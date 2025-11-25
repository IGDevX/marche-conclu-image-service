package org.igdevx.imageservice.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.imageservice.model.Image;
import org.igdevx.imageservice.repository.ImageRepository;
import org.igdevx.imageservice.service.ImageService;
import org.igdevx.imageservice.service.MinioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Component
@Profile("dev-local")
@RequiredArgsConstructor
@Slf4j
public class ImageSeeder implements CommandLineRunner {

    private final ImageRepository imageRepository;
    private final MinioService minioService;
    private final ImageService imageService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🌱 Starting image seeding for dev-local profile...");

        cleanExistingData();
        createSeedImages();

        log.info("Image seeding completed successfully!");
    }

    private void cleanExistingData() {
        log.info("Cleaning existing data...");

        List<Image> existingImages = imageRepository.findAll();

        if (!existingImages.isEmpty()) {
            log.info("{} image(s) found in database, deleting...", existingImages.size());

            for (Image image : existingImages) {
                try {
                    minioService.deleteFile(image.getCloudPath());
                    log.debug("Deleted from MinIO: {}", image.getCloudPath());
                } catch (Exception e) {
                    log.warn("Error deleting {} from MinIO: {}",
                            image.getCloudPath(), e.getMessage());
                }
            }

            imageRepository.deleteAll();
            log.info("Database cleaned");
        } else {
            log.info("No existing data to clean");
        }
    }

    private void createSeedImages() {
        log.info("Creating seed images...");

        try {
            loadImagesFromFolder("seed-images/profiles", Image.EntityType.USER_PROFILE, "user-");
            loadImagesFromFolder("seed-images/banners", Image.EntityType.USER_BANNER, "user-");
            loadImagesFromFolder("seed-images/products", Image.EntityType.PRODUCT, "user-");

            long count = imageRepository.count();
            if (count > 0) {
                log.info("{} seed images created successfully", count);
            } else {
                log.warn("No seed images found. Add images to src/main/resources/seed-images/");
            }
        } catch (Exception e) {
            log.error("Error creating seed images", e);
        }
    }

    private void loadImagesFromFolder(String folderPath, Image.EntityType entityType, String userIdPrefix) {
        try {
            var resource = getClass().getClassLoader().getResource(folderPath);

            if (resource == null) {
                log.warn("Folder {} not found", folderPath);
                return;
            }

            var folder = new java.io.File(resource.toURI());
            var files = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png") ||
                name.toLowerCase().endsWith(".gif") ||
                name.toLowerCase().endsWith(".webp")
            );

            if (files == null || files.length == 0) {
                log.info("No images found in {}", folderPath);
                return;
            }

            log.info("Loading {} images from {}...", files.length, folderPath);

            int index = 1;
            for (var file : files) {
                try {
                    byte[] imageData = java.nio.file.Files.readAllBytes(file.toPath());
                    String contentType = determineContentType(file.getName());
                    String userId = userIdPrefix + String.format("%03d", index);
                    uploadSeedImage(entityType, userId, file.getName(), imageData, contentType);
                    index++;

                } catch (Exception e) {
                    log.error("Error loading {}: {}", file.getName(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error loading images from {}: {}", folderPath, e.getMessage());
        }
    }

    private String determineContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private void uploadSeedImage(Image.EntityType entityType, String userId, String fileName, byte[] imageData, String contentType) {
        try {
            MultipartFile multipartFile = new SeedMultipartFile(
                    "file",
                    fileName,
                    contentType,
                    imageData
            );

            String productId = null;
            if (entityType == Image.EntityType.PRODUCT) {
                productId = "product-" + UUID.randomUUID().toString().substring(0, 8);
            }

            imageService.uploadImage(multipartFile, entityType, userId, productId);
            log.info("{} - {} ({} bytes)", entityType, fileName, imageData.length);

        } catch (Exception e) {
            log.error("Error uploading {}: {}", fileName, e.getMessage());
        }
    }
}

