package org.igdevx.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    private UUID id;
    private String entityType;
    private String userId;
    private String productId;
    private String cloudPath;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime uploadedAt;
    private String url;
}

