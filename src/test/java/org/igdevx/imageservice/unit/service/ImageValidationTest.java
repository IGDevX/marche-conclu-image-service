package org.igdevx.imageservice.unit.service;

import org.igdevx.imageservice.UnitTest;
import org.igdevx.imageservice.model.Image;
import org.igdevx.imageservice.repository.ImageRepository;
import org.igdevx.imageservice.service.ImageService;
import org.igdevx.imageservice.service.MinioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("Image Validation Unit Tests")
class ImageValidationTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ImageService imageService;

    private static final String PRODUCTEUR_USER_ID = "producteur-001";

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"})
    @DisplayName("Should accept valid image content types")
    void validateImage_ValidContentTypes_NoException(String contentType) {
        // Given
        MultipartFile file = createMockImage("test.jpg", contentType, 2048);

        when(imageRepository.save(any())).thenAnswer(invocation -> {
            Image img = invocation.getArgument(0);
            img.setId(UUID.randomUUID());
            return img;
        });

        // When / Then
        assertThatCode(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            )
        ).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "application/pdf",
        "text/plain",
        "video/mp4",
        "application/json",
        "application/octet-stream",
        "text/html"
    })
    @DisplayName("Should reject invalid content types")
    void validateImage_InvalidContentTypes_ThrowsException(String contentType) {
        // Given
        MultipartFile file = createMockImage("document.pdf", contentType, 2048);

        // When / Then
        assertThatThrownBy(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File must be an image");
    }

    @Test
    @DisplayName("Should reject empty file")
    void validateImage_EmptyFile_ThrowsException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // When / Then
        assertThatThrownBy(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File is empty");
    }

    @Test
    @DisplayName("Should reject file larger than 10MB")
    void validateImage_FileTooLarge_ThrowsException() {
        // Given - 11MB file
        long elevenMB = 11 * 1024 * 1024;
        MultipartFile file = createMockImage("huge-image.jpg", "image/jpeg", elevenMB);

        // When / Then
        assertThatThrownBy(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.PRODUCT,
                PRODUCTEUR_USER_ID,
                "product-123"
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File size must not exceed 10 MB");
    }

    @Test
    @DisplayName("Should accept file exactly 10MB")
    void validateImage_ExactlyTenMB_Success() {
        // Given - Exactly 10MB file
        long tenMB = 10 * 1024 * 1024;
        MultipartFile file = createMockImage("large-image.jpg", "image/jpeg", tenMB);

        when(imageRepository.save(any())).thenAnswer(invocation -> {
            Image img = invocation.getArgument(0);
            img.setId(UUID.randomUUID());
            return img;
        });

        // When / Then
        assertThatCode(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_BANNER,
                PRODUCTEUR_USER_ID,
                null
            )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject file with null content type")
    void validateImage_NullContentType_ThrowsException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn(null);
        lenient().when(file.getSize()).thenReturn(2048L);

        // When / Then
        assertThatThrownBy(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File must be an image");
    }

    @Test
    @DisplayName("Should accept very small image (1KB)")
    void validateImage_VerySmallFile_Success() {
        // Given - 1KB file
        MultipartFile file = createMockImage("tiny.jpg", "image/jpeg", 1024);

        when(imageRepository.save(any())).thenAnswer(invocation -> {
            Image img = invocation.getArgument(0);
            img.setId(UUID.randomUUID());
            return img;
        });

        // When / Then
        assertThatCode(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            )
        ).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "photo.jpg",
        "image.jpeg",
        "banner.png",
        "profile.webp",
        "logo.gif",
        "photo-produit.jpg",
        "tomate_bio.png"
    })
    @DisplayName("Should accept various valid filenames")
    void validateImage_ValidFilenames_Success(String filename) {
        // Given
        MultipartFile file = createMockImage(filename, "image/jpeg", 2048);

        when(imageRepository.save(any())).thenAnswer(invocation -> {
            Image img = invocation.getArgument(0);
            img.setId(UUID.randomUUID());
            return img;
        });

        // When / Then
        assertThatCode(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.PRODUCT,
                PRODUCTEUR_USER_ID,
                "product-123"
            )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle file with fake image extension")
    void validateImage_FakeImageExtension_StillValidatesContentType() {
        // Given - Text file with .jpg extension
        MultipartFile file = createMockImage("malicious.jpg", "text/plain", 1024);

        // When / Then - Content type validation catches it
        assertThatThrownBy(() ->
            imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File must be an image");
    }

    // Helper method

    private MultipartFile createMockImage(String filename, String contentType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.isEmpty()).thenReturn(false);
        try {
            lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[(int) Math.min(size, 1024)]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}

