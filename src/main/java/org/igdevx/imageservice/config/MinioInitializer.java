package org.igdevx.imageservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.imageservice.service.MinioService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioInitializer {

    private final MinioService minioService;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void initializeBucket() {
        log.info("Initializing MinIO bucket: {}", minioConfig.getBucketName());
        try {
            minioService.ensureBucketExists();
            log.info("MinIO bucket '{}' initialized successfully", minioConfig.getBucketName());
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", minioConfig.getBucketName(), e);
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }
}

