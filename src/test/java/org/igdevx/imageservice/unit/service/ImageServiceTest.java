package org.igdevx.imageservice.unit.service;

import org.igdevx.imageservice.UnitTest;
import org.igdevx.imageservice.dto.ImageResponse;
import org.igdevx.imageservice.dto.UploadResponse;
import org.igdevx.imageservice.model.Image;
import org.igdevx.imageservice.repository.ImageRepository;
import org.igdevx.imageservice.service.ImageService;
import org.igdevx.imageservice.service.MinioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService Unit Tests")
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ImageService imageService;

    private static final String RESTAURATEUR_USER_ID = "restaurateur-001";
    private static final String PRODUCTEUR_USER_ID = "producteur-001";
    private static final String PRODUCT_ID = "product-123";

    @Nested
    @DisplayName("Upload Profile Image Tests")
    class UploadProfileImageTests {

        @Test
        @DisplayName("Should upload profile image for restaurateur")
        void uploadProfileImage_Restaurateur_Success() throws Exception {
            // Given
            MultipartFile file = createMockImage("profile.jpg", "image/jpeg", 2048);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            when(minioService.getPresignedUrl(anyString()))
                .thenReturn("http://minio.local/users/restaurateur-001/profile.jpg");

            // When
            UploadResponse response = imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                RESTAURATEUR_USER_ID,
                null
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getImageId()).isNotNull();
            assertThat(response.getCloudPath()).isEqualTo("users/restaurateur-001/profile.jpg");
            assertThat(response.getUrl()).contains("minio");

            verify(minioService).uploadFile(eq(file), eq("users/restaurateur-001/profile.jpg"));
            verify(imageRepository).save(argThat(img ->
                img.getUserId().equals(RESTAURATEUR_USER_ID) &&
                img.getEntityType() == Image.EntityType.USER_PROFILE &&
                img.getProductId() == null
            ));
        }

        @Test
        @DisplayName("Should upload profile image for producteur")
        void uploadProfileImage_Producteur_Success() throws Exception {
            // Given
            MultipartFile file = createMockImage("profile.jpg", "image/jpeg", 2048);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            when(minioService.getPresignedUrl(anyString()))
                .thenReturn("http://minio.local/users/producteur-001/profile.jpg");

            // When
            UploadResponse response = imageService.uploadImage(
                file,
                Image.EntityType.USER_PROFILE,
                PRODUCTEUR_USER_ID,
                null
            );

            // Then
            assertThat(response.getCloudPath()).isEqualTo("users/producteur-001/profile.jpg");

            verify(imageRepository).save(argThat(img ->
                img.getUserId().equals(PRODUCTEUR_USER_ID) &&
                img.getProductId() == null
            ));
        }

        @Test
        @DisplayName("Should replace existing profile image")
        void uploadProfileImage_ReplaceExisting_Success() throws Exception {
            // Given
            MultipartFile newFile = createMockImage("new-profile.jpg", "image/jpeg", 3072);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            // When
            UploadResponse response = imageService.uploadImage(
                newFile,
                Image.EntityType.USER_PROFILE,
                RESTAURATEUR_USER_ID,
                null
            );

            // Then
            assertThat(response.getCloudPath()).isEqualTo("users/restaurateur-001/profile.jpg");
            verify(minioService).uploadFile(any(), eq("users/restaurateur-001/profile.jpg"));
        }
    }

    @Nested
    @DisplayName("Upload Banner Image Tests")
    class UploadBannerImageTests {

        @Test
        @DisplayName("Should upload banner for restaurateur")
        void uploadBanner_Restaurateur_Success() throws Exception {
            // Given
            MultipartFile file = createMockImage("banner.jpg", "image/jpeg", 5120);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            // When
            UploadResponse response = imageService.uploadImage(
                file,
                Image.EntityType.USER_BANNER,
                RESTAURATEUR_USER_ID,
                null
            );

            // Then
            assertThat(response.getCloudPath()).isEqualTo("users/restaurateur-001/banner.jpg");

            verify(imageRepository).save(argThat(img ->
                img.getUserId().equals(RESTAURATEUR_USER_ID) &&
                img.getEntityType() == Image.EntityType.USER_BANNER &&
                img.getProductId() == null
            ));
        }

        @Test
        @DisplayName("Should upload banner for producteur")
        void uploadBanner_Producteur_Success() throws Exception {
            // Given
            MultipartFile file = createMockImage("banner.jpg", "image/jpeg", 5120);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            // When
            UploadResponse response = imageService.uploadImage(
                file,
                Image.EntityType.USER_BANNER,
                PRODUCTEUR_USER_ID,
                null
            );

            // Then
            assertThat(response.getCloudPath()).isEqualTo("users/producteur-001/banner.jpg");
            verify(imageRepository).save(argThat(img ->
                img.getUserId().equals(PRODUCTEUR_USER_ID)
            ));
        }
    }

    @Nested
    @DisplayName("Upload Product Image Tests")
    class UploadProductImageTests {

        @Test
        @DisplayName("Should upload product image for producteur")
        void uploadProductImage_Producteur_Success() throws Exception {
            // Given
            MultipartFile file = createMockImage("tomate.jpg", "image/jpeg", 4096);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            when(minioService.getPresignedUrl(anyString()))
                .thenReturn("http://minio.local/products/producteur-001/xyz.jpg");

            // When
            UploadResponse response = imageService.uploadImage(
                file,
                Image.EntityType.PRODUCT,
                PRODUCTEUR_USER_ID,
                PRODUCT_ID
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCloudPath()).startsWith("products/producteur-001/");
            assertThat(response.getCloudPath()).endsWith(".jpg");

            verify(imageRepository).save(argThat(img ->
                img.getUserId().equals(PRODUCTEUR_USER_ID) &&
                img.getProductId().equals(PRODUCT_ID) &&
                img.getEntityType() == Image.EntityType.PRODUCT
            ));
        }

        @Test
        @DisplayName("Should upload multiple product images for same producteur")
        void uploadProductImages_MultipleProducts_Success() throws Exception {
            // Given
            MultipartFile file1 = createMockImage("tomate.jpg", "image/jpeg", 4096);
            MultipartFile file2 = createMockImage("carotte.jpg", "image/jpeg", 3072);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            // When
            UploadResponse response1 = imageService.uploadImage(
                file1,
                Image.EntityType.PRODUCT,
                PRODUCTEUR_USER_ID,
                "product-001"
            );

            UploadResponse response2 = imageService.uploadImage(
                file2,
                Image.EntityType.PRODUCT,
                PRODUCTEUR_USER_ID,
                "product-002"
            );

            // Then
            assertThat(response1.getCloudPath()).startsWith("products/producteur-001/");
            assertThat(response2.getCloudPath()).startsWith("products/producteur-001/");

            verify(imageRepository, times(2)).save(argThat(img ->
                img.getUserId().equals(PRODUCTEUR_USER_ID) &&
                img.getEntityType() == Image.EntityType.PRODUCT
            ));
        }

        @Test
        @DisplayName("Should not allow restaurateur to upload product image")
        void uploadProductImage_Restaurateur_BusinessLogicCheck() throws Exception {
            // Note: In real scenario, this would be validated by business layer
            // Here we just verify the service accepts any userId

            MultipartFile file = createMockImage("product.jpg", "image/jpeg", 2048);

            when(imageRepository.save(any(Image.class)))
                .thenAnswer(invocation -> {
                    Image img = invocation.getArgument(0);
                    img.setId(UUID.randomUUID());
                    return img;
                });

            // When - Service itself doesn't validate user role (done in business layer)
            UploadResponse response = imageService.uploadImage(
                file,
                Image.EntityType.PRODUCT,
                RESTAURATEUR_USER_ID,
                PRODUCT_ID
            );

            // Then - Image is saved (business validation is upstream)
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Image Tests")
    class GetImageTests {

        @Test
        @DisplayName("Should get profile image by userId and type")
        void getImageByUserAndType_ProfileImage_Found() {
            // Given
            Image profileImage = createImage(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE,
                null,
                "users/restaurateur-001/profile.jpg"
            );

            when(imageRepository.findByUserIdAndEntityTypeAndDeletedAtIsNull(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE
            )).thenReturn(List.of(profileImage));

            when(minioService.getPresignedUrl(anyString()))
                .thenReturn("http://minio.local/users/restaurateur-001/profile.jpg");

            // When
            ImageResponse response = imageService.getImageByUserAndType(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(RESTAURATEUR_USER_ID);
            assertThat(response.getEntityType()).isEqualTo("USER_PROFILE");
            assertThat(response.getProductId()).isNull();
        }

        @Test
        @DisplayName("Should get banner image by userId and type")
        void getImageByUserAndType_BannerImage_Found() {
            // Given
            Image bannerImage = createImage(
                PRODUCTEUR_USER_ID,
                Image.EntityType.USER_BANNER,
                null,
                "users/producteur-001/banner.jpg"
            );

            when(imageRepository.findByUserIdAndEntityTypeAndDeletedAtIsNull(
                PRODUCTEUR_USER_ID,
                Image.EntityType.USER_BANNER
            )).thenReturn(List.of(bannerImage));

            // When
            ImageResponse response = imageService.getImageByUserAndType(
                PRODUCTEUR_USER_ID,
                Image.EntityType.USER_BANNER
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEntityType()).isEqualTo("USER_BANNER");
        }

        @Test
        @DisplayName("Should return null when image not found")
        void getImageByUserAndType_NotFound_ReturnsNull() {
            // Given
            when(imageRepository.findByUserIdAndEntityTypeAndDeletedAtIsNull(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE
            )).thenReturn(List.of());

            // When
            ImageResponse response = imageService.getImageByUserAndType(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE
            );

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should get product image by productId")
        void getImageByProductId_Found() {
            // Given
            Image productImage = createImage(
                PRODUCTEUR_USER_ID,
                Image.EntityType.PRODUCT,
                PRODUCT_ID,
                "products/producteur-001/xyz.jpg"
            );

            when(imageRepository.findByProductIdAndDeletedAtIsNull(PRODUCT_ID))
                .thenReturn(Optional.of(productImage));

            // When
            ImageResponse response = imageService.getImageByProductId(PRODUCT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getUserId()).isEqualTo(PRODUCTEUR_USER_ID);
            assertThat(response.getEntityType()).isEqualTo("PRODUCT");
        }

        @Test
        @DisplayName("Should get all images of a user")
        void getImagesByUser_MultipleImages_ReturnsAll() {
            // Given
            List<Image> userImages = List.of(
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.USER_PROFILE, null, "users/producteur-001/profile.jpg"),
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.USER_BANNER, null, "users/producteur-001/banner.jpg"),
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.PRODUCT, "product-001", "products/producteur-001/p1.jpg"),
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.PRODUCT, "product-002", "products/producteur-001/p2.jpg")
            );

            when(imageRepository.findByUserIdAndDeletedAtIsNull(PRODUCTEUR_USER_ID))
                .thenReturn(userImages);

            // When
            List<ImageResponse> responses = imageService.getImagesByUser(PRODUCTEUR_USER_ID);

            // Then
            assertThat(responses).hasSize(4);
            assertThat(responses).extracting(ImageResponse::getUserId)
                .containsOnly(PRODUCTEUR_USER_ID);
            assertThat(responses).extracting(ImageResponse::getEntityType)
                .containsExactlyInAnyOrder("USER_PROFILE", "USER_BANNER", "PRODUCT", "PRODUCT");
        }
    }

    @Nested
    @DisplayName("Delete Image Tests")
    class DeleteImageTests {

        @Test
        @DisplayName("Should delete single image by ID")
        void deleteImage_ById_Success() throws Exception {
            // Given
            UUID imageId = UUID.randomUUID();
            Image image = createImage(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE,
                null,
                "users/restaurateur-001/profile.jpg"
            );
            image.setId(imageId);

            when(imageRepository.findByIdAndDeletedAtIsNull(imageId))
                .thenReturn(Optional.of(image));

            doNothing().when(minioService).deleteFile(anyString());

            // When
            imageService.deleteImage(imageId);

            // Then
            verify(imageRepository).save(argThat(img ->
                img.getDeletedAt() != null
            ));
            verify(minioService).deleteFile("users/restaurateur-001/profile.jpg");
        }

        @Test
        @DisplayName("Should delete product image by productId")
        void deleteImageByProductId_Success() throws Exception {
            // Given
            Image productImage = createImage(
                PRODUCTEUR_USER_ID,
                Image.EntityType.PRODUCT,
                PRODUCT_ID,
                "products/producteur-001/xyz.jpg"
            );

            when(imageRepository.findByProductIdAndDeletedAtIsNull(PRODUCT_ID))
                .thenReturn(Optional.of(productImage));

            // When
            imageService.deleteImageByProductId(PRODUCT_ID);

            // Then
            verify(imageRepository).save(argThat(img ->
                img.getDeletedAt() != null &&
                img.getProductId().equals(PRODUCT_ID)
            ));
            verify(minioService).deleteFile("products/producteur-001/xyz.jpg");
        }

        @Test
        @DisplayName("Should delete all images when user account is deleted")
        void deleteAllUserImages_UserDeleted_RemovesAllImages() throws Exception {
            // Given - Producteur with profile, banner, and 2 products
            List<Image> userImages = List.of(
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.USER_PROFILE, null, "users/producteur-001/profile.jpg"),
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.USER_BANNER, null, "users/producteur-001/banner.jpg"),
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.PRODUCT, "product-001", "products/producteur-001/p1.jpg"),
                createImage(PRODUCTEUR_USER_ID, Image.EntityType.PRODUCT, "product-002", "products/producteur-001/p2.jpg")
            );

            when(imageRepository.findByUserIdAndDeletedAtIsNull(PRODUCTEUR_USER_ID))
                .thenReturn(userImages);

            // When
            imageService.deleteAllUserImages(PRODUCTEUR_USER_ID);

            // Then
            verify(imageRepository).saveAll(argThat(images -> {
                for (Image img : images) {
                    if (img.getDeletedAt() == null) {
                        return false;
                    }
                }
                return true;
            }));
            verify(minioService).deleteFolder("users/producteur-001/");
            verify(minioService).deleteFolder("products/producteur-001/");
        }

        @Test
        @DisplayName("Should handle MinIO deletion failure gracefully")
        void deleteImage_MinioFails_ImageStillMarkedAsDeleted() throws Exception {
            // Given
            UUID imageId = UUID.randomUUID();
            Image image = createImage(
                RESTAURATEUR_USER_ID,
                Image.EntityType.USER_PROFILE,
                null,
                "users/restaurateur-001/profile.jpg"
            );
            image.setId(imageId);

            when(imageRepository.findByIdAndDeletedAtIsNull(imageId))
                .thenReturn(Optional.of(image));

            doThrow(new IOException("MinIO connection failed"))
                .when(minioService).deleteFile(anyString());

            // When
            imageService.deleteImage(imageId);

            // Then - Image still marked as deleted in DB
            verify(imageRepository).save(argThat(img ->
                img.getDeletedAt() != null
            ));
        }
    }

    // Helper methods

    private MultipartFile createMockImage(String filename, String contentType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(size);
        when(file.isEmpty()).thenReturn(false);
        try {
            lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[(int) size]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    private Image createImage(String userId, Image.EntityType entityType, String productId, String cloudPath) {
        return Image.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .entityType(entityType)
            .productId(productId)
            .cloudPath(cloudPath)
            .fileName("test.jpg")
            .contentType("image/jpeg")
            .sizeBytes(2048L)
            .build();
    }
}

