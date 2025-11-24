package org.igdevx.imageservice.unit.service;

import io.minio.*;
import io.minio.messages.Item;
import org.igdevx.imageservice.UnitTest;
import org.igdevx.imageservice.config.MinioConfig;
import org.igdevx.imageservice.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
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
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("MinioService Unit Tests")
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioConfig minioConfig;

    @InjectMocks
    private MinioService minioService;

    private static final String BUCKET_NAME = "marche-conclu-images-test";
    private static final String ENDPOINT = "http://localhost:9000";

    @BeforeEach
    void setUp() {
        lenient().when(minioConfig.getBucketName()).thenReturn(BUCKET_NAME);
        lenient().when(minioConfig.getEndpoint()).thenReturn(ENDPOINT);
    }

    @Nested
    @DisplayName("Bucket Management Tests")
    class BucketManagementTests {

        @Test
        @DisplayName("Should create bucket if not exists")
        void ensureBucketExists_BucketNotFound_CreatesBucket() throws Exception {
            // Given
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenReturn(false);

            doNothing().when(minioClient).makeBucket(any(MakeBucketArgs.class));
            doNothing().when(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));

            // When
            minioService.ensureBucketExists();

            // Then
            verify(minioClient).bucketExists(any(BucketExistsArgs.class));
            verify(minioClient).makeBucket(any(MakeBucketArgs.class));
            verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
        }

        @Test
        @DisplayName("Should not create bucket if already exists")
        void ensureBucketExists_BucketExists_DoesNotCreateAgain() throws Exception {
            // Given
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenReturn(true);

            doNothing().when(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));

            // When
            minioService.ensureBucketExists();

            // Then
            verify(minioClient).bucketExists(any(BucketExistsArgs.class));
            verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
            verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
        }

        @Test
        @DisplayName("Should throw exception when bucket creation fails")
        void ensureBucketExists_CreationFails_ThrowsException() throws Exception {
            // Given
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("Connection failed"));

            // When / Then
            assertThatThrownBy(() -> minioService.ensureBucketExists())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to create bucket");
        }
    }

    @Nested
    @DisplayName("File Upload Tests")
    class FileUploadTests {

        @Test
        @DisplayName("Should upload profile image to correct path")
        void uploadFile_ProfileImage_Success() throws Exception {
            // Given
            String cloudPath = "users/producteur-001/profile.jpg";
            MultipartFile file = createMockFile("profile.jpg", "image/jpeg", 2048);

            when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));

            // When
            String result = minioService.uploadFile(file, cloudPath);

            // Then
            assertThat(result).isEqualTo(cloudPath);
            verify(minioClient).putObject(argThat(args ->
                args.bucket().equals(BUCKET_NAME) &&
                args.object().equals(cloudPath)
            ));
        }

        @Test
        @DisplayName("Should upload product image to correct path")
        void uploadFile_ProductImage_Success() throws Exception {
            // Given
            String cloudPath = "products/producteur-001/tomate.jpg";
            MultipartFile file = createMockFile("tomate.jpg", "image/jpeg", 3072);

            when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));

            // When
            String result = minioService.uploadFile(file, cloudPath);

            // Then
            assertThat(result).isEqualTo(cloudPath);
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw exception when upload fails")
        void uploadFile_MinioError_ThrowsException() throws Exception {
            // Given
            String cloudPath = "users/restaurateur-001/banner.jpg";
            MultipartFile file = createMockFile("banner.jpg", "image/jpeg", 2048);

            doThrow(new RuntimeException("MinIO error"))
                .when(minioClient).putObject(any(PutObjectArgs.class));

            // When / Then
            assertThatThrownBy(() -> minioService.uploadFile(file, cloudPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Error uploading to MinIO");
        }
    }

    @Nested
    @DisplayName("File Download Tests")
    class FileDownloadTests {

        @Test
        @DisplayName("Should download file from MinIO")
        void downloadFile_Success() throws Exception {
            // Given
            String cloudPath = "users/producteur-001/profile.jpg";
            GetObjectResponse mockResponse = mock(GetObjectResponse.class);

            when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(mockResponse);

            // When
            InputStream result = minioService.downloadFile(cloudPath);

            // Then
            assertThat(result).isNotNull();
            verify(minioClient).getObject(argThat(args ->
                args.bucket().equals(BUCKET_NAME) &&
                args.object().equals(cloudPath)
            ));
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void downloadFile_FileNotFound_ThrowsException() throws Exception {
            // Given
            String cloudPath = "users/nonexistent/profile.jpg";

            when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("File not found"));

            // When / Then
            assertThatThrownBy(() -> minioService.downloadFile(cloudPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Error downloading from MinIO");
        }
    }

    @Nested
    @DisplayName("File Deletion Tests")
    class FileDeletionTests {

        @Test
        @DisplayName("Should delete single file from MinIO")
        void deleteFile_Success() throws Exception {
            // Given
            String cloudPath = "users/restaurateur-001/profile.jpg";

            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // When
            minioService.deleteFile(cloudPath);

            // Then
            verify(minioClient).removeObject(argThat(args ->
                args.bucket().equals(BUCKET_NAME) &&
                args.object().equals(cloudPath)
            ));
        }

        @Test
        @DisplayName("Should call MinIO to list and delete folder contents")
        void deleteFolder_UserFolder_Success() throws Exception {
            // Given
            String folderPath = "users/producteur-001/";

            when(minioClient.listObjects(any(ListObjectsArgs.class)))
                .thenReturn(new EmptyIterable());

            // When
            minioService.deleteFolder(folderPath);

            // Then
            verify(minioClient).listObjects(argThat(args ->
                args.bucket().equals(BUCKET_NAME) &&
                args.prefix().equals(folderPath) &&
                args.recursive()
            ));
        }

        // Helper class for empty iterable
        private static class EmptyIterable implements Iterable<Result<Item>> {
            @Override
            public java.util.Iterator<Result<Item>> iterator() {
                return java.util.Collections.emptyIterator();
            }
        }

        @Test
        @DisplayName("Should throw exception when deletion fails")
        void deleteFile_MinioError_ThrowsException() throws Exception {
            // Given
            String cloudPath = "users/producteur-001/banner.jpg";

            doThrow(new RuntimeException("MinIO error"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // When / Then
            assertThatThrownBy(() -> minioService.deleteFile(cloudPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Error deleting from MinIO");
        }
    }

    @Nested
    @DisplayName("URL Generation Tests")
    class URLGenerationTests {

        @Test
        @DisplayName("Should generate public URL for profile image")
        void getPresignedUrl_ProfileImage_ReturnsPublicURL() {
            // Given
            String cloudPath = "users/restaurateur-001/profile.jpg";

            // When
            String url = minioService.getPresignedUrl(cloudPath);

            // Then
            assertThat(url).isEqualTo("http://localhost:9000/marche-conclu-images-test/users/restaurateur-001/profile.jpg");
        }

        @Test
        @DisplayName("Should generate public URL for product image")
        void getPresignedUrl_ProductImage_ReturnsPublicURL() {
            // Given
            String cloudPath = "products/producteur-001/tomate.jpg";

            // When
            String url = minioService.getPresignedUrl(cloudPath);

            // Then
            assertThat(url).contains(ENDPOINT);
            assertThat(url).contains(BUCKET_NAME);
            assertThat(url).contains(cloudPath);
        }

        @Test
        @DisplayName("Should generate URL without expiration token")
        void getPresignedUrl_PublicMode_NoExpirationToken() {
            // Given
            String cloudPath = "users/producteur-001/banner.jpg";

            // When
            String url = minioService.getPresignedUrl(cloudPath);

            // Then
            assertThat(url).doesNotContain("X-Amz-Expires");
            assertThat(url).doesNotContain("X-Amz-Signature");
            assertThat(url).isEqualTo(ENDPOINT + "/" + BUCKET_NAME + "/" + cloudPath);
        }
    }

    // Helper methods

    private MultipartFile createMockFile(String filename, String contentType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn(size);
        try {
            lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[(int) size]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}

