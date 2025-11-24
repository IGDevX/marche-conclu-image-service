# Image Service

Microservice for managing image uploads and storage using MinIO.

## Overview

This service provides a unified interface for uploading, storing, and retrieving images for user profiles, banners, and products. Images are stored in MinIO and metadata is persisted in PostgreSQL.

## How it works

1. Client uploads an image via REST API
2. Service validates the file (type, size)
3. Image is uploaded to MinIO (public bucket)
4. Metadata is stored in PostgreSQL
5. Service returns image ID and permanent public URL
6. Client stores image ID and URL in its own database

## API Documentation

Swagger UI is available at:
```
http://localhost:5023/swagger-ui.html
```

## Main Endpoints

### Upload
```
POST /upload/profile
POST /upload/banner
POST /upload/product
```

### Retrieve
```
GET /{id}
GET /entity/{type}/{entityId}
```

### Delete
```
DELETE /{id}
DELETE /entity/{entityId}
```

## Integration with other services

See **INTEGRATION.md** for detailed integration guide with user-service and shop-service.

## Quick Start

```bash
# Start dependencies
docker-compose up -d

# Run service
mvn spring-boot:run -Dspring-boot.run.profiles=dev-local
```

## Tech Stack

- Java 17
- Spring Boot 3.5.6
- MinIO 8.5.7
- PostgreSQL
- Flyway

