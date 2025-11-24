package org.igdevx.imageservice.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.imageservice.config.MinioConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("Bucket created: {}", minioConfig.getBucketName());
                setBucketPublicReadPolicy();
            } else {
                setBucketPublicReadPolicy();
            }
        } catch (Exception e) {
            log.error("Error while checking/creating bucket", e);
            throw new RuntimeException("Unable to create bucket", e);
        }
    }

    public String uploadFile(MultipartFile file, String cloudPath) throws IOException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(cloudPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded: {}", cloudPath);
            return cloudPath;

        } catch (Exception e) {
            log.error("Error uploading file: {}", cloudPath, e);
            throw new IOException("Error uploading to MinIO", e);
        }
    }

    public void uploadBytes(byte[] data, String cloudPath, String contentType) throws IOException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(cloudPath)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Data uploaded: {}", cloudPath);

        } catch (Exception e) {
            log.error("Error uploading data: {}", cloudPath, e);
            throw new IOException("Error uploading to MinIO", e);
        }
    }

    public InputStream downloadFile(String cloudPath) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(cloudPath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file: {}", cloudPath, e);
            throw new IOException("Error downloading from MinIO", e);
        }
    }

    public void deleteFile(String cloudPath) throws IOException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(cloudPath)
                            .build()
            );

            log.info("File deleted: {}", cloudPath);

        } catch (Exception e) {
            log.error("Error deleting file: {}", cloudPath, e);
            throw new IOException("Error deleting from MinIO", e);
        }
    }

    public String getPresignedUrl(String cloudPath) {
        String endpoint = minioConfig.getEndpoint();
        String bucketName = minioConfig.getBucketName();
        String publicUrl = String.format("%s/%s/%s", endpoint, bucketName, cloudPath);

        log.debug("Public URL generated: {}", publicUrl);
        return publicUrl;
    }

    public void setBucketPublicReadPolicy() {
        try {
            String policy = String.format("""
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """, minioConfig.getBucketName());

            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .config(policy)
                    .build()
            );

            log.info("Bucket {} configured in public mode (read-only)", minioConfig.getBucketName());

        } catch (Exception e) {
            log.warn("Unable to configure bucket as public. Configure it manually via MinIO Console.", e);
        }
    }

    public void deleteFolder(String folderPath) throws IOException {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .prefix(folderPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(item.objectName())
                                .build()
                );
                log.debug("Deleted MinIO object: {}", item.objectName());
            }

            log.info("Folder deleted from MinIO: {}", folderPath);

        } catch (Exception e) {
            log.error("Error deleting folder from MinIO: {}", folderPath, e);
            throw new IOException("Error deleting folder from MinIO", e);
        }
    }
}

